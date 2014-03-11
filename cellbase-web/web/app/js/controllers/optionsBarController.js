var optionsBarControl = myApp.controller('optionsBarController', ['$scope', '$rootScope','mySharedService', 'CellbaseService', function ($scope, $rootScope, mySharedService, CellbaseService) {

    //this will be obtained from cellbase
    $scope.species = [
        {longName: "Homo sapiens", shortName: "hsapiens", ensemblName: "Homo_sapiens", data: "gene,variation,regulation"},
        {longName: "Mus musculus", shortName: "mmusculus", ensemblName: "Mus_musculus", data: "gene"},
        {longName: "Rattus norvegicus", shortName: "rnorvegicus", data: "gene"},
        {longName: "Danio rerio", shortName: "drerio", ensembleName: "Danio_rerio", data: "gene"},
        {longName: "Drosophila melanogaster", shortName: "dmelanogaster", ensembleName: "Drosophila_melanogaster", data: "gene,variation"},
        {longName: "Caenorhabditis elegans", shortName: "celegans", ensembleName: "Caenorhabditis_elegans", data: "gene"},
        {longName: "Saccharomyces cerevisiae", shortName: "scerevisiae", ensembleName: "Saccharomyces_cerevisiae", data: "gene"},
        {longName: "Canis familiaris", shortName: "cfamiliaris", ensembleName: "Canis_familiaris", data: "gene"},
        {longName: "Sus scrofa", shortName: "sscrofa", ensembleName: "Sus_scrofa", ensembleName: "Sus_scrofa", data: "gene"},
        {longName: "Anopheles gambiae", shortName: "agambiae", data: "gene"},
        {longName: "Plasmodium falciparum", shortName: "pfalciparum", data: "gene"}
    ];

    $scope.selectedSpecie = "Homo sapiens";

    //comunicate the new specie selected
    $scope.setSelectedSpecie = function (specie) {
        $scope.selectedSpecie = specie.longName;
        mySharedService.broadcastSpecie(specie);
    };

    //------------------EVENTS-------------------
    //obtain the specie of genome viewer and take the complete information of the specie
    $scope.$on('genesSpecieGV', function () {

        for(var i in $scope.species){
            if($scope.species[i].longName == mySharedService.genesSpecieGV){
                mySharedService.broadcastGenesNew($scope.species[i]);
            }
        }
    });
    $scope.$on('variantsSpecieGV', function () {

        for(var i in $scope.species){
            if($scope.species[i].longName == mySharedService.variantsSpecieGV){
                mySharedService.broadcastVariantsNew($scope.species[i]);
            }
        }
    });
}]);

optionsBarControl.$inject = ['$scope', 'mySharedService'];

