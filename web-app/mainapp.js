var iris = angular.module("irisApp", [ "ngRoute", "ngResource", "ngTable", 
                                       "ui.bootstrap", "cfp.hotkeys", "treeControl", 
                                       "ngDraggable", "openlayers-directive", "ngSanitize", 
                                       "ng-breadcrumbs",
                                       "com.2fdevs.videogular",
                           			   "com.2fdevs.videogular.plugins.controls",
                        		       "com.2fdevs.videogular.plugins.overlayplay",
                        		       "com.2fdevs.videogular.plugins.poster" ]);

// include application wide route-specific cheat sheets
iris.config(["hotkeysProvider", function(hotkeysProvider) {
    hotkeysProvider.includeCheatSheet = true;
}]);

// log configuration
iris.config(["$logProvider", function($logProvider){
	$logProvider.debugEnabled(true);
}]);

iris.run(["$route", "$rootScope", "$location", function ($route, $rootScope, $location) {
    var original = $location.path;
    $location.path = function (path, reload) {
        if (reload === false) {
            var lastRoute = $route.current;
            var un = $rootScope.$on('$locationChangeSuccess', function () {
                $route.current = lastRoute;
                un();
            });
        }
        return original.apply($location, [path]);
    };
}]);

iris.config(["$routeProvider", "$locationProvider", function($routeProvider, $locationProvider) {
	$routeProvider.when("/", {
		templateUrl : "views/welcome.html",
		label : "Home"
	});
//	$routeProvider.when("/map", {
//		templateUrl : "views/map.html",
//	});
	$routeProvider.when("/keys", {
		templateUrl : "views/keys.html",
		label : "Keys"
	});
	$routeProvider.when("/projects", {
		templateUrl : "views/projects.html",
		label : "Projects"
	});
	$routeProvider.when("/project/:projectID/images", {
		templateUrl : "views/images.html",
		label : "Images"
	});
	$routeProvider.when("/project/:projectID/image/:imageID/label/", {
		templateUrl : "views/labeling.html",
		label : "Labeling"
	});
//	$routeProvider.when("/project/:projectID/image/:imageID/label/:annID", {
//		templateUrl : "views/labeling.html"
//	});
	$routeProvider.when("/project/:projectID/image/:imageID/gallery", {
		templateUrl : "views/annotationGallery.html",
		label : "Gallery"
	});
	$routeProvider.when("/project/:projectID/gallery", {
		templateUrl : "views/annotationGallery.html",
		label : "Gallery"
	});
	$routeProvider.when("/about", {
		templateUrl : "views/about.html",
		label : "About"
	});
	// default route (unknown)
	$routeProvider.otherwise({
		templateUrl : "views/404.html"
	});
}]);