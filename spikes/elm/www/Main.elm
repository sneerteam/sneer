import Color (..)
import Graphics.Collage (..)
import Graphics.Element (..)
import Signal (..)
import Time (..)
import Window
import Random
import Mouse
import Text (asText)
import List
import Easing
import Easing (ease, pair, float)
import Html
import Html.Attributes (class)

type alias Drone = {pos: Pos, target: Pos}

type alias Game = {drones: List Drone, seed: Random.Seed}

type alias Pos = (Float, Float)

type Event = Tick Time | Click (Int, Int)

swarmSize = 5

main =
  let game   = foldp update initialGame events
      events = merge clicks ticks
      clicks = Click <~ sampleOn Mouse.clicks mouse
      ticks  = Tick <~ fps 24
      mouse  = relativeMouse <~ map center Window.dimensions ~ Mouse.position
  in screen <~ Window.dimensions ~ game

update e g =
  case e of
    Click clickPos ->
      let drones = List.map2 retargetDrone g.drones targets
          retargetDrone drone t = {drone | target <- t}
          targets = List.map aroundClickPos randomDistances
          aroundClickPos = vecToFloat << vecAdd clickPos
          (randomDistances, seed') = Random.generate randomPairs g.seed
      in {g | seed <- seed'
            , drones <- drones}

    Tick delta ->
      let moveDrone drone = {drone | pos <- animate drone}
          duration = 2 * second
          interpolation = Easing.linear
          animate {pos, target} = ease interpolation (pair float) pos target duration delta
      in {g | drones <- List.map moveDrone g.drones}

initialDrone = {pos = (0, 0), target = (0, 0)}

initialGame = {drones = List.repeat swarmSize initialDrone
              ,seed = Random.initialSeed 42}

screen (sw, sh) {drones} =
  collage sw sh (List.map drone drones)
    |> container sw sh middle

drone {pos, target} =
  move pos
    <| lookAt pos target
    <| toForm
    <| Html.toElement 63 64 (Html.div [class "bee"] [])

lookAt (x, y) (p, q) =
  let angle = atan2 (x - p) (q - y)
  in rotate angle

randomPairs =
  Random.list swarmSize randomPair

randomPair =
  let range = Random.int -45 45
  in Random.pair range range

relativeMouse : (Int, Int) -> (Int, Int) -> (Int, Int)
relativeMouse (ox, oy) (x, y) = (x - ox, -(y - oy))

center : (Int, Int) -> (Int, Int)
center (w, h) = (w // 2, h // 2)

vecAdd (x, y) (p, q) = (x + p, y + q)

vecToFloat (x, y) = (toFloat x, toFloat y)
