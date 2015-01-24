import Color (..)
import Graphics.Collage (..)
import Graphics.Element (..)
import Signal (..)
import Time (..)
import Window
import Random
import Mouse
import Text (asText)

main = update <~ every (second / 24) ~ Window.dimensions ~ Mouse.position

update t (sw, sh) mp =
  let rp = relativeMouse (center (sw, sh)) mp
  in collage 400 400
      [ drone t rp ]
     |> container sw sh middle

drone : Time -> (Int, Int) -> Form
drone t (mx, my) =
  let (dx, dy) = randomPos (round t)
      pos = (toFloat <| mx + dx, toFloat <| my + dy)
  in move pos
     <| rotate (degrees 33)
     <| scale 0.25
     <| toForm <| image 315 345 beeImage
     -- <| filled yellow (circle 10)

beeImage = "http://fc02.deviantart.net/fs71/f/2013/010/b/a/bab078636bf6f05e6f7fd05af518d1a6-d5r3cyw.gif"

randomPos t =
  let range = Random.int -20 20
      seed  = Random.initialSeed t
      (pair, seed') = Random.generate (Random.pair range range) seed
  in pair

relativeMouse : (Int, Int) -> (Int, Int) -> (Int, Int)
relativeMouse (ox, oy) (x, y) = (x - ox, -(y - oy))

center : (Int, Int) -> (Int, Int)
center (w, h) = (w // 2, h // 2)
