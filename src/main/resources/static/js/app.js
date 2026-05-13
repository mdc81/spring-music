angular.module('SpringMusic', ['albums', 'errors', 'status', 'info', 'storage', 'ngRoute', 'ui.directives']).
    config(function ($locationProvider, $routeProvider) {
        // $locationProvider.html5Mode(true);

        $routeProvider.when('/errors', {
            controller: 'ErrorsController',
            templateUrl: 'templates/errors.html'
        });
        $routeProvider.when('/storage', {
            controller: 'StorageController',
            templateUrl: 'templates/storage.html'
        });
        $routeProvider.otherwise({
            controller: 'AlbumsController',
            templateUrl: 'templates/albums.html'
        });
    }
);
