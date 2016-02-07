# quake2d
2D game bot logic simulator inspired by Quake game series

##### Screens
<img src="/screens/screen1.png" width="350">
<img src="/screens/screen2.png" width="350">

##### Credits
Tomas Uktveris & Remigijus Valys</br>
Kaunas University of Technology 2010

##### Prerequisites
Java RE or SDK installed

##### Running simulator
Launch the `run_game.bat` script

##### Sample maps
Example maps for the game are in `example_maps` folder.

##### Running map editor
Launch the `example_maps/Map Editor/run.bat`

##### Building maps using Map editor
1. Open the map editor.
2. Place walls/health/ammo (use middle mouse button to change item value).
3. After the map is complete - place a `node` inside the map walkable area (not outside).</br>
`Note`: the node is used as a seed/starting point for automated graph generation in the game.
4. Press `save`, to save the map to current directory.

`Note:` map editor loads the map from current directory also.

##### Inspired by:
1. Quake games
2. Great book: 
`Mat Buckland, Programming Game AI by Example. Wordware Publishing, Texas, 2005`
