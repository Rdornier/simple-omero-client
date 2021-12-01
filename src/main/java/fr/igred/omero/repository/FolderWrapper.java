/*
 *  Copyright (C) 2020-2021 GReD
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.

 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51 Franklin
 * Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */

package fr.igred.omero.repository;


import fr.igred.omero.Client;
import fr.igred.omero.exception.AccessException;
import fr.igred.omero.exception.ExceptionHandler;
import fr.igred.omero.exception.OMEROServerError;
import fr.igred.omero.exception.ServiceException;
import fr.igred.omero.roi.ROIWrapper;
import omero.gateway.exception.DSAccessException;
import omero.gateway.exception.DSOutOfServiceException;
import omero.gateway.facility.ROIFacility;
import omero.gateway.model.FolderData;
import omero.gateway.model.ROIData;
import omero.gateway.model.ROIResult;
import omero.gateway.model.TagAnnotationData;
import omero.model.Folder;
import omero.model.FolderAnnotationLink;
import omero.model.FolderAnnotationLinkI;
import omero.model.FolderI;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static fr.igred.omero.exception.ExceptionHandler.handleServiceAndAccess;
import static fr.igred.omero.exception.ExceptionHandler.handleServiceAndServer;


/**
 * Class containing a FolderData.
 * <p> Implements function using the FolderData contained.
 */
public class FolderWrapper extends GenericRepositoryObjectWrapper<FolderData> {

    public static final String ANNOTATION_LINK = "FolderAnnotationLink";

    /** Id of the associated image */
    private long imageId = -1L;


    /**
     * Constructor of the FolderWrapper class.
     *
     * @param folder FolderData to contain.
     */
    public FolderWrapper(FolderData folder) {
        super(folder);
    }


    /**
     * Constructor of the FolderWrapper class.
     *
     * @param folder Folder to contain.
     */
    public FolderWrapper(Folder folder) {
        super(new FolderData(folder));
    }


    /**
     * Constructor of the FolderWrapper class. Save the folder in OMERO
     *
     * @param client The client handling the connection.
     * @param name   Name of the folder.
     *
     * @throws ServiceException Cannot connect to OMERO.
     * @throws OMEROServerError Server error.
     */
    public FolderWrapper(Client client, String name) throws ServiceException, OMEROServerError {
        super(new FolderData());
        data.setName(name);
        Folder f = (Folder) handleServiceAndServer(client.getGateway(),
                                                   g -> g.getUpdateService(client.getCtx())
                                                         .saveAndReturnObject(data.asIObject()),
                                                   "Could not create Folder with name: " + name);
        data.setFolder(f);
    }


    /**
     * Returns the type of annotation link for this object
     *
     * @return See above.
     */
    @Override
    protected String annotationLinkType() {
        return ANNOTATION_LINK;
    }


    /**
     * Private function. Adds a tag to the object in OMERO, if possible.
     *
     * @param client  The client handling the connection.
     * @param tagData Tag to be added.
     *
     * @throws ServiceException   Cannot connect to OMERO.
     * @throws AccessException    Cannot access data.
     * @throws ExecutionException A Facility can't be retrieved or instantiated.
     */
    @Override
    protected void addTag(Client client, TagAnnotationData tagData)
    throws ServiceException, AccessException, ExecutionException {
        FolderAnnotationLink link = new FolderAnnotationLinkI();
        link.setChild(tagData.asAnnotation());
        link.setParent(new FolderI(data.getId(), false));
        client.save(link);
    }


    /**
     * Gets the folder contained in the FolderWrapper
     *
     * @return the FolderData.
     */
    public FolderData asFolderData() {
        return data;
    }


    /**
     * Gets the name of the folder
     *
     * @return name.
     */
    public String getName() {
        return data.getName();
    }


    /**
     * Sets the name of the folder.
     *
     * @param name The name of the folder. Mustn't be {@code null}.
     *
     * @throws IllegalArgumentException If the name is {@code null}.
     */
    public void setName(String name) {
        data.setName(name);
    }


    /**
     * Gets the folder description
     *
     * @return The folder description.
     */
    public String getDescription() {
        return data.getDescription();
    }


    /**
     * Sets the description of the folder.
     *
     * @param description The folder description.
     */
    public void setDescription(String description) {
        data.setDescription(description);
    }


    /**
     * Sets the image associated to the folder
     *
     * @param id Id of the image to associate.
     */
    public void setImage(long id) {
        imageId = id;
    }


    /**
     * Sets the image associated to the folder
     *
     * @param image Image to associate.
     */
    public void setImage(ImageWrapper image) {
        imageId = image.getId();
    }


    /**
     * Add an ROI to the folder and associate it to the image id set(an image need to be associated)
     *
     * @param client The client handling the connection.
     * @param roi    ROI to add.
     *
     * @throws ServiceException   Cannot connect to OMERO.
     * @throws AccessException    Cannot access data.
     * @throws ExecutionException If the ROIFacility can't be retrieved or instantiated.
     */
    public void addROI(Client client, ROIWrapper roi)
    throws ServiceException, AccessException, ExecutionException {
        ROIFacility roiFac = client.getRoiFacility();
        handleServiceAndAccess(roiFac,
                               rf -> rf.addRoisToFolders(client.getCtx(),
                                                         imageId,
                                                         Collections.singletonList(roi.asROIData()),
                                                         Collections.singletonList(data)),
                               "Cannot add ROI to " + this);
    }


    /**
     * Gets the ROI contained in the folder associated with the image id set (an image need to be associated)
     *
     * @param client The client handling the connection.
     *
     * @return List of ROIWrapper containing the ROI.
     *
     * @throws ServiceException   Cannot connect to OMERO.
     * @throws AccessException    Cannot access data.
     * @throws ExecutionException A Facility can't be retrieved or instantiated.
     */
    public List<ROIWrapper> getROIs(Client client)
    throws ServiceException, AccessException, ExecutionException {
        ROIFacility roiFac = client.getRoiFacility();

        Collection<ROIResult> roiResults = handleServiceAndAccess(roiFac,
                                                                  rf -> rf.loadROIsForFolder(client.getCtx(), imageId,
                                                                                             data.getId()),
                                                                  "Cannot get ROIs from " + this);

        List<ROIWrapper> roiWrappers = new ArrayList<>(roiResults.size());
        if (!roiResults.isEmpty()) {
            ROIResult r = roiResults.iterator().next();

            Collection<ROIData> rois = r.getROIs();
            for (ROIData roi : rois) {
                ROIWrapper temp = new ROIWrapper(roi);
                roiWrappers.add(temp);
            }
        }

        return roiWrappers;
    }


    /**
     * Unlink all ROI, associated to the image set, in the folder. ROIs are now linked to the image directly
     *
     * @param client The client handling the connection.
     *
     * @throws ServiceException   Cannot connect to OMERO.
     * @throws AccessException    Cannot access data.
     * @throws ExecutionException A Facility can't be retrieved or instantiated.
     */
    public void unlinkAllROI(Client client) throws ServiceException, AccessException, ExecutionException {
        String           error = "Cannot unlink ROIs from " + this;
        ROIFacility      rf    = client.getRoiFacility();
        List<ROIWrapper> rois  = getROIs(client);
        for (ROIWrapper roi : rois) {
            ExceptionHandler.of(roi, error)
                            .apply(r -> rf.removeRoisFromFolders(client.getCtx(),
                                                                 this.imageId,
                                                                 Collections.singletonList(r.asROIData()),
                                                                 Collections.singletonList(data)))
                            .propagate(DSOutOfServiceException.class, ServiceException::new)
                            .propagate(DSAccessException.class, AccessException::new);
        }
    }


    /**
     * Unlink an ROI, associated to the image set, in the folder. the ROI is now linked to the image directly
     *
     * @param client The client handling the connection.
     * @param roi    ROI to unlink.
     *
     * @throws ServiceException   Cannot connect to OMERO.
     * @throws AccessException    Cannot access data.
     * @throws ExecutionException A Facility can't be retrieved or instantiated.
     */
    public void unlinkROI(Client client, ROIWrapper roi)
    throws ServiceException, AccessException, ExecutionException {
        String error = "Cannot unlink ROIs from " + this;
        ExceptionHandler.of(client.getRoiFacility(), error)
                        .apply(rf -> rf.removeRoisFromFolders(client.getCtx(),
                                                              this.imageId,
                                                              Collections.singletonList(roi.asROIData()),
                                                              Collections.singletonList(data)))
                        .propagate(DSOutOfServiceException.class, ServiceException::new)
                        .propagate(DSAccessException.class, AccessException::new);
    }

}