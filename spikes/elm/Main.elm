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

type alias Bee = {pos: Pos, target: Pos}

type alias Game = {bees: List Bee}

type alias Pos = (Float, Float)

main =
  let bee = foldp update initialGame events
      time = fps 24
      screenCenter = center <~ Window.dimensions
      mouse = relativeMouse <~ screenCenter ~ clicks
      clicks = sampleOn Mouse.clicks Mouse.position
      events = (,) <~ time ~ mouse
  in screen <~ Window.dimensions ~ bee

swarmSize = 5

update (t, (mx, my)) g =
  let poss = List.map pos <| randomPos swarmSize (round t)
      pos (dx, dy) = (toFloat <| mx + dx, toFloat <| my + dy)
      bee pos = {initialBee | pos <- pos}
  in {g | bees <- List.map bee poss}

initialBee = {pos = (0, 0), target = (0, 0)}

initialGame = {bees = List.repeat swarmSize initialBee}

screen (sw, sh) {bees} =
  collage 400 400 (List.map drone bees)
    |> container sw sh middle

drone {pos} =
  move pos <| rotate (degrees 33)
           <| scale 0.1
           <| toForm <| image 315 345 beeImage

beeImage = "http://fc02.deviantart.net/fs71/f/2013/010/b/a/bab078636bf6f05e6f7fd05af518d1a6-d5r3cyw.gif"

randomPos n t =
  let range = Random.int -30 30
      seed  = Random.initialSeed t
      pos   = Random.pair range range
      (pairs, seed') = Random.generate (Random.list n pos) seed
  in pairs

relativeMouse : (Int, Int) -> (Int, Int) -> (Int, Int)
relativeMouse (ox, oy) (x, y) = (x - ox, -(y - oy))

center : (Int, Int) -> (Int, Int)
center (w, h) = (w // 2, h // 2)
