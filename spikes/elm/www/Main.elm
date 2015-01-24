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

type alias Bee = {pos: Pos, target: Pos}

type alias Game = {bees: List Bee, seed: Random.Seed}

type alias Pos = (Float, Float)

type Event = Tick Time
           | Click (Int, Int)

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
      let bees = List.map2 retargetBee g.bees targets
          retargetBee bee t = {bee | target <- t}
          targets = List.map aroundClickPos randomDistances
          aroundClickPos = vecToFloat << vecAdd clickPos
          (randomDistances, seed') = Random.generate randomPairs g.seed
      in {g | seed <- seed'
            , bees <- bees}

    Tick delta ->
      let moveBee bee = {bee | pos <- animate bee}
          duration = 3 * second
          interpolation = Easing.linear
          animate {pos, target} = ease interpolation (pair float) pos target duration delta
      in {g | bees <- List.map moveBee g.bees}

vecAdd (x, y) (p, q) = (x + p, y + q)

vecToFloat (x, y) = (toFloat x, toFloat y)

initialBee = {pos = (0, 0), target = (0, 0)}

initialGame = {bees = List.repeat swarmSize initialBee
              ,seed = Random.initialSeed 42}

screen (sw, sh) {bees} =
  collage sw sh (List.map drone bees)
    |> container sw sh middle

drone {pos} =
  move pos <| rotate (degrees 33)
           <| toForm <| image 21 21 beeImage

beeImage = "img/abeia1.png"

--beeImage = "http://fc02.deviantart.net/fs71/f/2013/010/b/a/bab078636bf6f05e6f7fd05af518d1a6-d5r3cyw.gif"

randomPairs =
  Random.list swarmSize randomPair

randomPair =
  let range = Random.int -30 30
  in Random.pair range range

relativeMouse : (Int, Int) -> (Int, Int) -> (Int, Int)
relativeMouse (ox, oy) (x, y) = (x - ox, -(y - oy))

center : (Int, Int) -> (Int, Int)
center (w, h) = (w // 2, h // 2)
