package com.example.mapwithroute

class MapData {
    var routes = ArrayList<Routes>()
}

class Routes {
    var legs = ArrayList<Legs>()
}

class Legs {
    var steps = ArrayList<Steps>()
}

class Steps {
    var polyline = PolyLine()
}

class PolyLine {
    var points = ""
}


