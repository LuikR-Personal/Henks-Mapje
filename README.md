# Henks-Mapje
This is a public repo for the minecraft paper plugin Henks Mapje. This plugin introduces a vanilla style worldmap and more advanced minimap based on the Minecraft map item. Functionalities include dynamic zooming, custom waypoints and user defined map sizes

## Functionalities 
- World map. A combination of map items placed on a frame that render the world together. Introduces zoom functionality, asteatic frames and a seperate legend that can be used to set markers on the map.
- Minimap. A personal, custom map item which functions as a more vanilla friendly minimap. Introduces zoom and waypoints.
- Legend Book. A personal, custom Book and Quill item the user can use to save waypoints in a predefined format. These waypoints are then used to populate minimap and worldmap markers and waypoints.

## Usage 
- The worldmap is set up in the following way:
  1. The user receives a set of custom map items and two legend map items by using **/minimap group** where items can be dropped if they dont fit free inventory space. These are numbered in the item name.
  2. The map items can then be placed on frames where the numbering starts top left and moves right and down.
  3. The legend map items should be placed together with the map showing 'waypoints' on the right.
  4. The Worldmap is discovered through looking at the worldmap and using **/minimap import_chunks**.
  5. The Markers are imported through looking at the worldmap, Book and Quill in main hand and using **/minimap import_waypoints**
- The Waypoint book is set up in the following way:
  1. The user receives a custom Book and Quill by using **/minimap give_waypoints**. This Book and Quill should not be signed
  2. The user can then start saving waypoints in the following format: **X, Y, Z[Label]**. Take into account spaces are not allowed, maximum label size is 10 characters.
- The Personal Minimap is set up in the following way:
  1. The user receives a personal minimap map item by using **/minimap give_personal**
  2. This minimap updates fog as the player explores.
 
## Configuration of Worldmap and Minimap 
The worldmap and minimap can both be zoomed by either having the minimap in main hand or looking at a worldmap and using **/minimap zoom [1-50]**
The worldmap can be cosmetically configured in the following ways: 
1. Space between legend rows can be set by using **/minimap spacing[4-12]**
2. Icon size of the legend icons can be set by using **/minimap icon_scale[1-4]**
3. Icon size of the markers on the worldmap can be set by using **/minimap world_iconscale[1-4]**
