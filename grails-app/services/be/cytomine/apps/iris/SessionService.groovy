package be.cytomine.apps.iris

import org.codehaus.groovy.grails.web.json.JSONElement;

import grails.converters.JSON;
import grails.transaction.Transactional
import be.cytomine.client.Cytomine
import be.cytomine.client.CytomineException;

/**
 * This service handles all CRUD operations of a Session object.
 * It also communicates with the Cytomine host instance, if 
 * required. Thus passing the <code>request['cytomine']</code> instance 
 * is mandatory for some class methods.
 * 
 * @author Philipp Kainz
 *
 */
@Transactional
class SessionService {
	
	def projectService
	def grailsApplication

	@Transactional(readOnly = true)
	List<Session> getAll(){
		// fetch all sessions from the database where the user is owner
		List<Session> allSessions = Session.getAll()
		return allSessions
	}

	/**
	 * Gets the Session for a user identified by public key. 
	 * If the user has no Session yet, a new one will be created.
	 * The Session's state gets resolved and all <code>current</code> 
	 * instances are fetched freshly from Cytomine and injected into the JSON object.
	 * 
	 * @param cytomine a Cytomine instance
	 * @param publicKey the user's public key
	 * @return a Session as JSON object ready to render it to the client
	 */
	def get(Cytomine cytomine, String publicKey){
		be.cytomine.client.models.User cmUser = cytomine.getUser(publicKey)
		User user = User.findByCmID(cmUser.getId())

		// generate a new user, if the user does not yet exist, otherwise update the
		// user information from cytomine
		user = new DomainMapper().mapUser(cmUser, user)

		// try to fetch the session for this user
		Session userSession = user.getSession()
		if (userSession == null){
			userSession = new Session()
			user.setSession(userSession)
		}

		// save the user and the session
		user.save(flush:true,failOnError:true)


		// ############################################
		// inject non-IRIS objects into the session
		def sessJSON = new Utils().modelToJSON(userSession);

		// inject the user
		sessJSON.user.cytomine = cmUser.getAttr()

		// inject current project
		Project cP = userSession.getCurrentProject()

		if (cP != null){
			def pj = cytomine.getProject(cP.getCmID()).getAttr()
			sessJSON.currentProject.cytomine = pj
		}
		Image cI = userSession.getCurrentImage()
		if (cI != null){
			def img = cytomine.getImageInstance(cI.getCmID()).getAttr()
			sessJSON.currentImage.cytomine = img
		}
		// TODO inject current annotation
		//		if (aID != null){
		//			def ann = cytomine.getAnnotation(aID).getAttr()
		//			sessJSON.currentAnnotation.cytomine = ann
		//		}

		// return the session as json object
		return sessJSON
	}

	/**
	 * Updates a project associated with an IRIS Session. 
	 * Also fetches the current project settings from the Cytomine host 
	 * and returns the updated project.
	 * 
	 * @param cytomine a Cytomine instance
	 * @param sessionID the IRIS session ID where the project belongs to
	 * @param projectID the Cytomine project ID for updating
	 * @return the updated IRIS project instance
	 * 
	 * @throws CytomineException if the project is is not available for the 
	 * querying user
	 */
	def touchProject(Cytomine cytomine, long sessionID, long cmProjectID) throws CytomineException{
		// find the project in the session
		Session sess = Session.get(sessionID)

		// find the nested project by projectID
		Project projectForUpdate = sess.getProjects().find { it.cmID == cmProjectID }

		// fetch the Cytomine project instance
		def cmProject = cytomine.getProject(cmProjectID)

		projectForUpdate = new DomainMapper().mapProject(cmProject, projectForUpdate)
		projectForUpdate.updateLastActivity()
		
		// save the project in order to have the ID returned in the response
		projectForUpdate.save(flush:true,failOnError:true)

		// trigger reordering of the projects in the session
		sess.addToProjects(projectForUpdate)
		sess.updateLastActivity()

		// inject the project here directly in order to avoid fetching it again
		// from Cytomine
		def projectJSON = injectCytomineProject(cytomine, projectForUpdate, cmProject)

		return projectJSON
	}

	/**
	 * Fetches the current project settings from the Cytomine host
	 * and returns the project. Does not save the project on the IRIS server.
	 *
	 * @param cytomine a Cytomine instance
	 * @param sessionID the IRIS session ID where the project belongs to
	 * @param cmProjectID the Cytomine project ID for updating
	 * @return the updated IRIS project instance
	 *
	 * @throws CytomineException if the project is is not available for the
	 * querying user
	 */
	def getProject(Cytomine cytomine, long sessionID, long cmProjectID) throws CytomineException{
		// find the project in the session
		Session sess = Session.get(sessionID)

		// find the nested project by projectID
		Project projectForUpdate = sess.getProjects().find { it.cmID == cmProjectID }

		def projectJSON = injectCytomineProject(cytomine, projectForUpdate, null)

		return projectJSON
	}

	/**
	 * Injects a Cytomine Project into the IRIS project.
	 * 
	 * @param cytomine a Cytomine instance
	 * @param irisProject the IRIS Project
	 * @param cmProject the cytomineProject, or <code>null</code>, if the project should be fetched from Cytomine
	 * @return a JSON object with the injected Cytomine project
	 * 
	 * @throws CytomineException if the project is is not available for the
	 * querying user
	 */
	def injectCytomineProject(Cytomine cytomine, Project irisProject, def cmProject) throws CytomineException{
		
		def projectJSON = new Utils().modelToJSON(irisProject)
		
		// fetch the Cytomine project instance
		if (cmProject == null){
			cmProject = cytomine.getProject(irisProject.cmID)
			projectJSON.cytomine = cmProject.getAttr()
		} else {
			if (cmProject['attr'] != null){
				projectJSON.cytomine = cmProject.getAttr()
			} else {
				projectJSON.cytomine = cmProject
			}
		}
		return projectJSON
	}
	
	/**
	 * Injects a Cytomine Annotation into the IRIS annotation.
	 *
	 * @param cytomine a Cytomine instance
	 * @param irisAnnotation the IRIS Annotation
	 * @param cmAnnotation the cytomine Annotation, or <code>null</code>, if the Annotation should be fetched from Cytomine
	 * @return a JSON object with the injected Cytomine annotation
	 *
	 * @throws CytomineException if the annotation is is not available for the
	 * querying user
	 */
	def injectCytomineAnnotation(Cytomine cytomine, Annotation irisAnnotation, def cmAnnotation) throws CytomineException{
		
		def annJSON = new Utils().modelToJSON(irisAnnotation)
		
		// fetch the Cytomine project instance
		if (cmAnnotation == null){
			cmAnnotation = cytomine.getAnnotation(irisAnnotation.cmID)
			annJSON.cytomine = cmAnnotation.getAttr()
		} else {
			if (cmAnnotation['attr'] != null){
				annJSON.cytomine = cmAnnotation.getAttr()
			} else {
				annJSON.cytomine = cmAnnotation
			}
		}
		return annJSON
	}
	
	/**
	 * Injects a Cytomine Image into the IRIS Image.
	 *
	 * @param cytomine a Cytomine instance
	 * @param irisImage the IRIS Image
	 * @param cmImage the Cytomine image, or <code>null</code>, if the image should be fetched from Cytomine
	 * @return a JSON object with the injected Cytomine project
	 *
	 * @throws CytomineException if the image is is not available for the
	 * querying user
	 */
	def injectCytomineImageInstance(Cytomine cytomine, Image irisImage, def cmImage) throws CytomineException{
		
		def imageJSON = new Utils().modelToJSON(irisImage)
		
		// fetch the Cytomine image instance
		if (cmImage == null){
			cmImage = cytomine.getImageInstance(irisImage.cmID)
			imageJSON.cytomine = cmImage.getAttr()
		} else {
			if (cmImage['attr'] != null){
				imageJSON.cytomine = cmImage.getAttr()
			} else {
				imageJSON.cytomine = cmImage
			}
		}
		return imageJSON
	}

	/**
	 * Update an existing IRIS Project in a session with a JSON object delivered in the payload. 
	 * 
	 * 
	 * @param cytomine a Cytomine instance
	 * @param sessionID the session ID
	 * @param irisProjectID the Cytomine project ID
	 * @param payload the IRIS project as JSON (incl. injected cytomine project at "payload.cytomine")
	 * @return
	 */
	def updateByIRISProject(Cytomine cytomine, long sessionID, long irisProjectID, def payload) throws Exception{
		// check if the user may access this project
		def cmProject = cytomine.getProject(payload['cytomine'].id);
		
		// get the cytomine project from the payload
		if (cmProject == null){
			log.error("This user is not allowed to access the project.")
			
			return
		}
		
		// get the session
		Session sess = Session.get(sessionID)
		if (sess == null){
			log.error("Cannot find session " + sessionID)
			
			return
		}

		// find the specific project
		def sessProjects = sess.getProjects();
		log.debug("Session "+ sess.id +  " has " + sessProjects.size() + " projects: " + sessProjects)

		// try to find the project
		Project project = sessProjects.find { it.id == irisProjectID }

		// if no project is available in this session, return null and cause error
		if (project == null){
			log.error("Cannot find project " + irisProjectID + " for session " + sessionID)
			
			return
		}

		// map all properties from json to the project
		project = project.updateByJSON(payload)
		
		// add the project to the session and cause reordering
		sess.addToProjects(project)
		sess.updateLastActivity()

		def pjJSON = injectCytomineProject(cytomine, project, cmProject)
		return pjJSON
	}
	
	/**
	 * Updates an image associated with an IRIS Session and Project.
	 *
	 * @param cytomine a Cytomine instance
	 * @param sessionID the IRIS session ID where the project belongs to
	 * @param cmProjectID the Cytomine project ID for updating
	 * @param cmImageID the Cytomine imageID for updating
	 * @return the updated IRIS image instance
	 *
	 * @throws CytomineException if the image is is not available for the
	 * querying user
	 */
	def touchImage(Cytomine cytomine, long sessionID, long cmProjectID, long cmImageID) throws CytomineException{
		// find the project in the session
		Session sess = Session.get(sessionID)

		// find the nested project by projectID
		Project irisProject = sess.getProjects().find { it.cmID == cmProjectID }
		
		// find the nested image by imageID
		Image imageForUpdate = irisProject.getImages().find { it.cmID == cmImageID }
		
		// fetch the image from Cytomine
		def cmImage = cytomine.getImageInstance(cmImageID)
		
		// Map the client model to the IRIS model
		imageForUpdate = new DomainMapper().mapImage(cmImage, imageForUpdate)
		imageForUpdate.updateLastActivity()
		// set the "goToURL"
		imageForUpdate.setGoToURL(grailsApplication.config.grails.cytomine.host + "/#tabs-image-" + cmProjectID + "-" + cmImageID + "-")
		
		// save the image in order to immediately reflect the ID
		imageForUpdate.save(failOnError:true, flush:true)
		
		// update the time stamps and re-order the most recent images 
		irisProject.addToImages(imageForUpdate)
		irisProject.updateLastActivity()
		
		sess.addToProjects(irisProject)
		sess.updateLastActivity()
		
		// inject the project here directly in order to avoid fetching it again
		// from Cytomine
		def imageJSON = injectCytomineImageInstance(cytomine, imageForUpdate, cmImage)
		
		return imageJSON
	}


	/**
	 * Removes a session from the database.
	 * 
	 * @param cytomine a Cytomine instance
	 * @param sessID the IRIS session ID
	 * 
	 * @return <code>null</code>, if the session does not exist
	 * @throws Exception if any error occurs during deletion
	 */
	def delete(long sessID) throws Exception{
		Session sess = Session.get(sessID)
		sess.delete(flush:true, failOnError:true)
	}
}
