package be.cytomine.apps.iris

import org.apache.log4j.Logger;

import be.cytomine.apps.iris.Image;
import be.cytomine.apps.iris.Project;
import be.cytomine.apps.iris.User;
import grails.converters.JSON;

/**
 * This class maps from the client domain models to the IRIS 
 * domain models.
 * 
 * @author Philipp Kainz
 *
 */
class DomainMapper {
	/**
	 * Map the be.cytomine.client.models.User model to the IRIS domain model of a user.
	 * @param cmUser
	 * @return 
	 */
	User mapUser(be.cytomine.client.models.User cmUser, User irisUser){
		if (irisUser == null){
			irisUser = new User()
		}
		println cmUser as JSON
		
		// map the properties from the client user to the IRIS user model
		irisUser.setCmID(cmUser.getId())
		irisUser.setCmUserName(cmUser.getStr("username"))
		irisUser.setCmLastName(cmUser.getStr("lastname"))
		irisUser.setCmFirstName(cmUser.getStr("firstname"))
		irisUser.setCmPublicKey(cmUser.getStr("publicKey"))
		irisUser.setCmPrivateKey(cmUser.getStr("privateKey"))
		irisUser.setCmEmail(cmUser.getStr("email"))
		irisUser.setCmClass(cmUser.getStr("class"))
		irisUser.setCmPasswordExpired(cmUser.getBool("passwordExpired"))
		irisUser.setCmDeleted(cmUser.getBool("deleted"))
		irisUser.setCmAdminByNow(cmUser.getBool("adminByNow"))
		irisUser.setCmAlgo(cmUser.getBool("algo"))
		irisUser.setCmAdmin(cmUser.getBool("admin"))
		irisUser.setCmGuestByNow(cmUser.getBool("guestByNow"))
		irisUser.setCmIsSwitched(cmUser.getBool("isSwitched"))
		irisUser.setCmGuest(cmUser.getBool("guest"))
		irisUser.setCmUser(cmUser.getBool("user"))
		
		return irisUser
	}
	
	Project mapProject(be.cytomine.client.models.Project cmProject, Project irisProject){
		if (irisProject == null){
			irisProject = new Project()
		}
		
		irisProject.setCmID(cmProject.getId())
		irisProject.setCmName(cmProject.getStr("name"))
		irisProject.setCmBlindMode(cmProject.getBool("blindMode"))
		
		return irisProject
		
	}
	
	Project mapImage(be.cytomine.client.models.ImageInstance cmImage, Image irisImage){
		if (irisImage == null){
			irisImage = new Image()
		}
		
		irisImage.setCmID(cmImage.getId())
		irisImage.setCmName(cmImage.getStr("name"))
		
		return irisImage
		
	}
}