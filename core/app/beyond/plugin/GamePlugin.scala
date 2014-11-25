package beyond.plugin

import beyond.engine.javascript.AssetsModuleSourceProvider
import beyond.engine.javascript.BeyondGlobal
import beyond.engine.javascript.BeyondJavaScriptEngine
import beyond.engine.javascript.lib.ScriptableConsole
import beyond.engine.javascript.lib.ScriptableFuture
import beyond.engine.javascript.lib.http.ScriptableRequest
import beyond.engine.javascript.lib.http.ScriptableResponse
import com.typesafe.scalalogging.slf4j.{ StrictLogging => Logging }
import org.mozilla.javascript.Context
import org.mozilla.javascript.Function
import play.api.mvc.Request
import play.api.mvc.Result
import scala.concurrent.Future

class NoHandlerFunctionFoundException extends Exception

object GamePlugin extends Logging {
  import com.beyondframework.rhino.RhinoConversions._
  import scala.concurrent.ExecutionContext.Implicits.global

  private val engine = {
    val library = new AssetsModuleSourceProvider
    new BeyondJavaScriptEngine(new BeyondGlobal(library))
  }
  ScriptableConsole.setRedirectConsoleToLogger(true)

  private val handler: Function = engine.contextFactory.call { cx: Context =>
    val mainFilename = "main.js"
    val exports = engine.loadMain(mainFilename)
    // FIXME: Don't hardcode the name of handler function.
    val handler = exports.get("handle", exports)
    handler match {
      case _: Function =>
        handler
      case _ /* Scriptable.NOT_FOUND */ =>
        logger.error("No handler function is found")
        throw new NoHandlerFunctionFoundException
    }
  }.asInstanceOf[Function]

  def handle[A](request: Request[A]): Future[Result] = engine.contextFactory.call { cx: Context =>
    val scope = engine.global

    val args: Array[AnyRef] = Array(ScriptableRequest(cx, request))
    val response = handler.call(cx, scope, scope, args)
    response match {
      case f: ScriptableFuture =>
        f.future.mapTo[ScriptableResponse].map(_.result)
      case res: ScriptableResponse =>
        Future.successful(res.result)
    }
  }.asInstanceOf[Future[Result]]
}

