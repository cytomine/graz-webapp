/**
 * Created by lrollus on 7/14/14.
 */
var iris = angular.module("irisApp");

iris.constant("projectURL", "api/projects.json");
iris.constant("projectDescrURL", "api/project/{id}/description.json");
iris.constant("ontologyURL", "api/ontology/{ontologyID}.json");
iris.constant("ontologyByProjectURL", "api/project/{projectID}/ontology.json");
iris.constant("projectUsersURL", "api/project/{projectID}/users.json");

iris.factory("projectService", [
	"$http", "$log",
	"projectURL",
	"projectDescrURL",
	"ontologyURL",
	"ontologyByProjectURL",
	"projectUsersURL",
	"sessionService",
	"cytomineService",
	    function($http, $log,
		projectURL, 
		projectDescrURL, 
		ontologyURL, 
		ontologyByProjectURL,
		projectUsersURL,
		sessionService,
		cytomineService) {

	return {

		// retrieve one specific project
		getDescription : function(projectID, callbackSuccess, callbackError) {
			var tmpUrl = projectDescrURL.replace("{id}", projectID);
			$http.get(cytomineService.addKeys(tmpUrl)).success(function(data) {
				// console.log("success on $http.get(" + tmpUrl + ")");
				if (callbackSuccess) {
					callbackSuccess(data);
				}
			}).error(function(data, status, headers, config) {
				// console.log(callbackError)
				if (callbackError) {
					callbackError(data, status, headers, config);
				}
			})
		},

		// refresh all projects (fetch the entire collection freshly from the
		// Cytomine core server
		fetchProjects : function(callbackSuccess, callbackError) {
			var url = cytomineService.addKeys(projectURL);

			$http.get(url).success(function(data) {
				// $log.debug("success on $http.get(" + url + ")");
				if (callbackSuccess) {
					callbackSuccess(data);
				}
			}).error(function(data, status, headers, config) {
				// on error log the error
				if (callbackError) {
					callbackError(data, status, headers, config);
				}
			})
		},
		
		// get the ontology
		fetchOntology : function(ontologyID, params, callbackSuccess, callbackError) {
			var url = cytomineService.addKeys(ontologyURL).replace("{ontologyID}", ontologyID);
			if (params !== null){
				if (params.flat == true) {
					url += "&flat=true";
				}	
			}
				
			$http.get(url).success(function(data) {
				// $log.debug("success on $http.get(" + url + ")");
				// on success, assign the data to the projects array
				if (callbackSuccess) {
					callbackSuccess(data);
				}
			}).error(function(data, status, headers, config) {
				// on error log the error
				if (callbackError) {
					callbackError(data, status, headers, config);
				}
			})
		},

		// get the associated ontology for a project
		fetchOntologyByProjectID : function(projectID, params, callbackSuccess, callbackError) {
			var url = cytomineService.addKeys(ontologyByProjectURL).replace("{projectID}", projectID);
			if (params !== null){
				if (params.flat == true) {
					url += "&flat=true";
				}
			}

			$http.get(url).success(function(data) {
				// $log.debug("success on $http.get(" + url + ")");
				// on success, assign the data to the projects array
				if (callbackSuccess) {
					callbackSuccess(data);
				}
			}).error(function(data, status, headers, config) {
				// on error log the error
				if (callbackError) {
					callbackError(data, status, headers, config);
				}
			})
		},

		// get the ontology
		fetchUsers : function(projectID, params, callbackSuccess, callbackError) {
			var url = cytomineService.addKeys(projectUsersURL).replace("{projectID}", projectID);
			if (params !== null){
				if (params.adminOnly == true) {
					url += "&adminOnly=true";
				}
			}

			$http.get(url).success(function(data) {
				// $log.debug("success on $http.get(" + url + ")");
				// on success, assign the data to the projects array
				if (callbackSuccess) {
					callbackSuccess(data);
				}
			}).error(function(data, status, headers, config) {
				// on error log the error
				if (callbackError) {
					callbackError(data, status, headers, config);
				}
			})
		}
	};
}]);