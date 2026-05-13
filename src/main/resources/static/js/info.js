angular.module('info', ['ngResource']).
    factory('Info', function ($resource) {
        return $resource('appinfo');
    });

function InfoController($scope, Info) {
    $scope.info = Info.get();

    $scope.isEcsEnabled = function () {
        return $scope.info && $scope.info.profiles &&
               $scope.info.profiles.indexOf('ecs') !== -1;
    };
}
