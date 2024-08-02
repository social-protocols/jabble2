package frontend.components

import outwatch.*
import outwatch.dsl.*

def parentThread(parents: Vector[rpc.Post]): VMod = {
  div(
    parents.map { parent =>
      div(
        cls := "w-full mb-2 flex items-start border-l-4 border-solid border-grey-500 pl-2 text-sm",
        parent.content,
      )
    }
  )
}
