package be.cytomine.apps.iris

import grails.converters.JSON

import org.json.simple.JSONArray
import org.json.simple.JSONObject

import be.cytomine.client.Cytomine
import be.cytomine.client.CytomineException
import be.cytomine.client.models.Ontology
import be.cytomine.client.models.User

/**
 * 
 * 
 * @author Loic Rollus, Philipp Kainz
 *
 */
class CytomineController {

	/**
	 * Get all projects from Cytomine host, which are associated with the user executing this query.
	 * @return a ProjectCollection as JSON object
	 */
	def getProjects() {
		// get the Cytomine instance from the request (injected by the security filter!)
		Cytomine cytomine = request['cytomine']
		def projectList = cytomine.getProjects().list

		// optionally resolve the ontology and inject it in the project
		if (params['resolveOntology'].equals("true")){
			// get the ontology object for each project
			projectList.each {
				//println it.get("ontology")
				long oID = it.get("ontology")
				def ontology = cytomine.getOntology(oID)
				it.resolvedOntology = ontology;
			}
		}
		render projectList as JSON
	}

	/**
	 * Build the URLs for the images by adding the proper suffix to the Cytomine host URL.
	 * The project ID is retrieved via the injected <code>params</code> property.
	 * @return the list of images for a given project ID as JSON object
	 */
	def getImages() {
		Cytomine cytomine = request['cytomine']

		long userID = cytomine.getUser(params.get("publicKey")).getId()
		long projectID = params.long("projectID");

		// TODO implement paging using max and offset parameters from the request params
		//int offset = params.long("offset")
		//int max = params.long("max")
		//cytomine.setMax(5); //max 5 images
		
		def imageList = cytomine.getImageInstances(params.long('projectID')).list
		imageList.each {
			//for each image, add a goToURL property containing the full URL to open the image in the core Cytomine instance
			it.goToURL = grailsApplication.config.grails.cytomine.host + "/#tabs-image-" + projectID + "-" + it.id + "-"

			// retrieve the user's progress on each image and return it in the object
			JSONObject annInfo = new Utils().getUserProgress(cytomine, projectID, it.id, userID)
			// resolving the values from the JSONObject to each image as property
			it.labeledAnnotations = annInfo.get("labeledAnnotations")
			it.userProgress = annInfo.get("userProgress")
		}
		render imageList as JSON
	}

	/**
	 * Gets the project description for a given ID.
	 * @return the description of a project as JSON object
	 */
	def getProjectDescription(){
		def description;
		try {
			//println "request arrived: " + params
			description = request['cytomine'].getDescription(params.long('projectID'), 'be.cytomine.project.Project')

			render description as JSON
		} catch (CytomineException e) {
			// 404 {"message":"Domain not found with id : 98851569 and className=be.cytomine.project.Project"}
			// println e
			// if there is no description associated, return a JSON object
			//			if (e.httpCode == 404){
			//				d = new JSONObject()
			//				d.put("data", "This project does not have any description.")
			//				d.put("status", 404);
			//			}
			//			println d as JSON
			//			render d as JSON
			log.error(e);
			
			// IMPORTANT HINT:
			// do not send any response, since the Grails server automatically sends a 404 code
			// and triggers the callbackError method in the JS client
		}
	}

	/**
	 * Gets an ontology by ID and optionally 'deflates' the hierarchy, if the 
	 * request <code>params</code> contain <code>flat=true</code>.
	 * @return the ontology as JSON object
	 */
	def getOntology(){
		Cytomine cytomine = request['cytomine']
		long oID = params.long('ontologyID')

		Ontology ontology = cytomine.getOntology(oID)

		if (params["flat"].equals("true")){
			List<JSONObject> flatOntology = new Utils().flattenOntology(ontology)
			render flatOntology as JSON
		} else {
			render ontology as JSON
		}
	}

	/**
	 * Gets a user which is identified by public key.
	 * @return the user as JSON object
	 */
	def getUserByPublicKey(){
		Cytomine cytomine = request['cytomine']
		String publicKey = params['pubKey']

		User user = cytomine.getUser(publicKey)

		render user.getAt("attr") as JSON
	}

	/**
	 * Gets the image server URLs for a given image.
	 * @return the URLs for a given abstractImage for the OpenLayers instance
	 */
	def getImageServerURLs(){
		Cytomine cytomine = request['cytomine']
		long abstrImgID = params.long('abstractImageID')
		long imgInstID = params.long('imageinstance')

		// perform a synchronous get request to the Cytomine host server
		def urls = cytomine.doGet("/api/abstractimage/" + abstrImgID + "/imageservers.json?imageinstance=" + imgInstID)
		urls.replace("\"", "\\\"")
		response.setContentType("application/json")
		response.setCharacterEncoding("UTF-8")
		render urls;
	}
}
