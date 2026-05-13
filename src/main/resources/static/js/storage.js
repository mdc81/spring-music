angular.module('storage', ['ngResource', 'status']).
    factory('StorageFiles', function ($resource) {
        return $resource('storage/files', {}, {
            query: { method: 'GET', isArray: true },
            remove: { method: 'DELETE', params: { key: '@key' } }
        });
    }).
    factory('AlbumFiles', function ($resource) {
        return $resource('storage/albums/:albumId/files', { albumId: '@albumId' }, {
            query: { method: 'GET', isArray: true }
        });
    });

function StorageController($scope, $http, StorageFiles, Status) {
    $scope.files = [];
    $scope.uploading = false;
    $scope.selectedFile = null;

    $scope.loadFiles = function () {
        StorageFiles.query(
            function (files) { $scope.files = files; },
            function () { Status.error('Failed to load files'); }
        );
    };

    $scope.onFileSelected = function (element) {
        $scope.$apply(function () {
            $scope.selectedFile = element.files[0] || null;
        });
    };

    $scope.upload = function () {
        if (!$scope.selectedFile) {
            Status.error('Please select a file first');
            return;
        }

        var formData = new FormData();
        formData.append('file', $scope.selectedFile);
        $scope.uploading = true;

        $http.post('storage/upload', formData, {
            headers: { 'Content-Type': undefined },
            transformRequest: angular.identity
        }).then(
            function () {
                Status.success('File uploaded successfully');
                $scope.selectedFile = null;
                document.getElementById('generalFileInput').value = '';
                $scope.loadFiles();
            },
            function (result) {
                Status.error('Upload failed: ' + (result.data && result.data.message ? result.data.message : result.status));
            }
        ).finally(function () {
            $scope.uploading = false;
        });
    };

    $scope.download = function (file) {
        window.location.href = 'storage/download?key=' + encodeURIComponent(file.key);
    };

    $scope.deleteFile = function (file) {
        $http.delete('storage/files', { params: { key: file.key } }).then(
            function () {
                Status.success('File deleted');
                $scope.loadFiles();
            },
            function () { Status.error('Failed to delete file'); }
        );
    };

    $scope.humanSize = function (bytes) {
        if (bytes === null || bytes === undefined) return '';
        if (bytes < 1024) return bytes + ' B';
        if (bytes < 1048576) return (bytes / 1024).toFixed(1) + ' KB';
        return (bytes / 1048576).toFixed(1) + ' MB';
    };

    $scope.loadFiles();
}

function AlbumFilesController($scope, $http, AlbumFiles, Status) {
    $scope.albumFiles = [];
    $scope.albumUploading = false;
    $scope.albumSelectedFile = null;
    $scope.showFiles = false;

    $scope.toggleFiles = function (album) {
        $scope.showFiles = !$scope.showFiles;
        if ($scope.showFiles) {
            $scope.loadAlbumFiles(album);
        }
    };

    $scope.loadAlbumFiles = function (album) {
        AlbumFiles.query({ albumId: album.id },
            function (files) { $scope.albumFiles = files; },
            function () { Status.error('Failed to load album files'); }
        );
    };

    $scope.onAlbumFileSelected = function (element) {
        $scope.$apply(function () {
            $scope.albumSelectedFile = element.files[0] || null;
        });
    };

    $scope.uploadForAlbum = function (album) {
        if (!$scope.albumSelectedFile) {
            Status.error('Please select a file first');
            return;
        }

        var formData = new FormData();
        formData.append('file', $scope.albumSelectedFile);
        $scope.albumUploading = true;

        $http.post('storage/albums/' + album.id + '/upload', formData, {
            headers: { 'Content-Type': undefined },
            transformRequest: angular.identity
        }).then(
            function () {
                Status.success('File attached to album');
                $scope.albumSelectedFile = null;
                $scope.loadAlbumFiles(album);
            },
            function (result) {
                Status.error('Upload failed: ' + (result.data && result.data.message ? result.data.message : result.status));
            }
        ).finally(function () {
            $scope.albumUploading = false;
        });
    };

    $scope.downloadAlbumFile = function (file) {
        window.location.href = 'storage/download?key=' + encodeURIComponent(file.key);
    };

    $scope.deleteAlbumFile = function (file, album) {
        $http.delete('storage/files', { params: { key: file.key } }).then(
            function () {
                Status.success('File deleted');
                $scope.loadAlbumFiles(album);
            },
            function () { Status.error('Failed to delete file'); }
        );
    };

    $scope.humanSize = function (bytes) {
        if (bytes === null || bytes === undefined) return '';
        if (bytes < 1024) return bytes + ' B';
        if (bytes < 1048576) return (bytes / 1024).toFixed(1) + ' KB';
        return (bytes / 1048576).toFixed(1) + ' MB';
    };
}
