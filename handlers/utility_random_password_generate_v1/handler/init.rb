require 'rexml/document'

class UtilityRandomPasswordGenerateV1
  def initialize(input)
    # Create a String of words that are 4 or 5 characters long.  The words in this
    # String must be separated by a new line because of the processing that takes
    # place in the execute method.
    @words = <<-WORDS
able
actor
alarm
anger
apple
army
aunt
badge
bait
bath
bead
beam
bean
beast
beef
bike
bird
bomb
book
boot
brain
bread
brick
brush
cable
camp
cast
cave
cent
chin
clam
class
cloth
club
coach
coast
coil
corn
crate
cream
crib
crook
crow
crowd
crown
deer
desk
dime
dirt
dock
dress
drug
drum
dust
elbow
face
fang
feast
field
fifth
fight
flag
flesh
flock
food
frame
frog
fruit
fuel
game
gate
geese
ghost
glove
glue
goose
grade
grain
grape
grass
guide
hair
heart
heat
hill
home
honey
hook
hope
horn
horse
hose
idea
jail
jeans
joke
judge
juice
kiss
kite
lace
lake
lamp
light
linen
loaf
lock
lunch
magic
maid
mask
meal
meat
mice
milk
mint
money
month
moon
music
name
nest
north
nose
ocean
pail
park
patch
pear
pest
plane
plant
plot
queen
quiet
quilt
rain
rake
rifle
river
road
robin
rock
room
rose
route
sack
sail
scale
scarf
scene
scent
seed
shape
sheet
shoe
shop
show
sink
skate
slave
sleet
smoke
snail
snake
snow
soap
soda
sofa
song
space
spark
spoon
spot
stage
star
step
stew
stove
straw
sugar
suit
swing
table
tank
team
tent
test
tiger
title
toad
toes
town
trail
tramp
tray
tree
trick
trip
twig
uncle
vase
veil
vein
vest
water
week
wheel
wing
wish
woman
women
wood
wool
wren
wrist
yard
year
zebra
    WORDS
  end

  def execute()
    # Randomly choose a word from @words string.
    word = @words.sort_by{rand}[0].chomp
    # Choose a random number between from 0 to 7999 and then add 1000 to the result.
    # The resulting extension will range from 1000 to 8999.
    extension = rand(8000)+1000
    # Append the new_pass and extension strings to get the result_pass string.
    result_password = "#{word}#{extension}"

    # Build and return the results string
    <<-RESULTS
    <results>
      <result name="temporary_password">#{result_password}</result>
    </results>
    RESULTS
  end

end