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
import fr.igred.omero.annotations.TagAnnotationWrapper;
import fr.igred.omero.exception.AccessException;
import fr.igred.omero.exception.OMEROServerError;
import fr.igred.omero.exception.ServiceException;
import omero.gateway.model.DatasetData;
import omero.gateway.model.ProjectData;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static fr.igred.omero.exception.ExceptionHandler.handleServiceAndAccess;


/**
 * Class containing a ProjectData
 * <p> Implements function using the Project contained
 */
public class ProjectWrapper extends GenericRepositoryObjectWrapper<ProjectData> {

    public static final String ANNOTATION_LINK = "ProjectAnnotationLink";


    /**
     * Constructor of the ProjectWrapper class.
     *
     * @param project ProjectData to be contained.
     */
    public ProjectWrapper(ProjectData project) {
        super(project);
    }


    /**
     * Creates a new project and save it to OMERO.
     *
     * @param client      The client handling the connection.
     * @param name        Project name.
     * @param description Project description.
     *
     * @return The project newly created.
     *
     * @throws ServiceException   Cannot connect to OMERO.
     * @throws AccessException    Cannot access data.
     * @throws ExecutionException A Facility can't be retrieved or instantiated.
     */
    public static ProjectWrapper create(Client client, String name, String description)
    throws ServiceException, AccessException, ExecutionException {
        ProjectWrapper project = new ProjectWrapper(new ProjectData());
        project.setName(name);
        project.setDescription(description);
        project.saveAndUpdate(client);
        return project;
    }


    /**
     * Gets all images in the dataset available from OMERO.
     *
     * @return ImageWrapper list.
     */
    private static List<ImageWrapper> purge(Collection<? extends ImageWrapper> images) {
        List<ImageWrapper> purged = new ArrayList<>(images.size());

        for (ImageWrapper image : images) {
            if (purged.isEmpty() || purged.get(purged.size() - 1).getId() != image.getId()) {
                purged.add(image);
            }
        }

        return purged;
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
     * Gets the ProjectData name
     *
     * @return ProjectData name.
     */
    public String getName() {
        return data.getName();
    }


    /**
     * Sets the name of the project.
     *
     * @param name The name of the project. Mustn't be {@code null}.
     *
     * @throws IllegalArgumentException If the name is {@code null}.
     */
    public void setName(String name) {
        data.setName(name);
    }


    /**
     * Gets the project description
     *
     * @return The project description.
     */
    public String getDescription() {
        return data.getDescription();
    }


    /**
     * Sets the description of the project.
     *
     * @param description The description of the project.
     */
    public void setDescription(String description) {
        data.setDescription(description);
    }


    /**
     * @return the ProjectData contained.
     */
    public ProjectData asProjectData() {
        return data;
    }


    /**
     * Gets all the datasets in the project available from OMERO.
     *
     * @return Collection of DatasetWrapper.
     */
    public List<DatasetWrapper> getDatasets() {
        return wrap(data.getDatasets(), DatasetWrapper::new, DatasetWrapper::getId);
    }


    /**
     * Gets the dataset with the specified name from OMERO
     *
     * @param name Name of the dataset searched.
     *
     * @return List of dataset with the given name.
     */
    public List<DatasetWrapper> getDatasets(String name) {
        List<DatasetWrapper> datasets = getDatasets();
        datasets.removeIf(dataset -> !dataset.getName().equals(name));
        return datasets;
    }


    /**
     * Adds a dataset to the project in OMERO. Create the dataset.
     *
     * @param client      The client handling the connection.
     * @param name        Dataset name.
     * @param description Dataset description.
     *
     * @return The object saved in OMERO.
     *
     * @throws ServiceException   Cannot connect to OMERO.
     * @throws AccessException    Cannot access data.
     * @throws ExecutionException A Facility can't be retrieved or instantiated.
     */
    public DatasetWrapper addDataset(Client client, String name, String description)
    throws ServiceException, AccessException, ExecutionException {
        DatasetData datasetData = new DatasetData();
        datasetData.setName(name);
        datasetData.setDescription(description);
        return addDataset(client, datasetData);
    }


    /**
     * Adds a dataset to the project in OMERO.
     *
     * @param client  The client handling the connection.
     * @param dataset Dataset to be added.
     *
     * @return The object saved in OMERO.
     *
     * @throws ServiceException   Cannot connect to OMERO.
     * @throws AccessException    Cannot access data.
     * @throws ExecutionException A Facility can't be retrieved or instantiated.
     */
    public DatasetWrapper addDataset(Client client, DatasetWrapper dataset)
    throws ServiceException, AccessException, ExecutionException {
        return addDataset(client, dataset.asDatasetData());
    }


    /**
     * Private function. Add a dataset to the project.
     *
     * @param client      The client handling the connection.
     * @param datasetData Dataset to be added.
     *
     * @return The object saved in OMERO.
     *
     * @throws ServiceException   Cannot connect to OMERO.
     * @throws AccessException    Cannot access data.
     * @throws ExecutionException A Facility can't be retrieved or instantiated.
     */
    private DatasetWrapper addDataset(Client client, DatasetData datasetData)
    throws ServiceException, AccessException, ExecutionException {
        datasetData.setProjects(Collections.singleton(data));
        DatasetWrapper newDataset = new DatasetWrapper(datasetData);
        newDataset.saveAndUpdate(client);
        refresh(client);
        return newDataset;
    }


    /**
     * Removes a dataset from the project in OMERO.
     *
     * @param client  The client handling the connection.
     * @param dataset Dataset to remove.
     *
     * @throws ServiceException     Cannot connect to OMERO.
     * @throws AccessException      Cannot access data.
     * @throws ExecutionException   A Facility can't be retrieved or instantiated.
     * @throws OMEROServerError     If the thread was interrupted.
     * @throws InterruptedException If block(long) does not return.
     */
    public void removeDataset(Client client, DatasetWrapper dataset)
    throws ServiceException, AccessException, ExecutionException, OMEROServerError, InterruptedException {
        removeLink(client, "ProjectDatasetLink", dataset.getId());
        refresh(client);
    }


    /**
     * Gets all images in the project available from OMERO.
     *
     * @param client The client handling the connection.
     *
     * @return ImageWrapper list.
     *
     * @throws ServiceException   Cannot connect to OMERO.
     * @throws AccessException    Cannot access data.
     * @throws ExecutionException A Facility can't be retrieved or instantiated.
     */
    public List<ImageWrapper> getImages(Client client) throws ServiceException, AccessException, ExecutionException {
        Collection<DatasetWrapper> datasets = getDatasets();

        List<ImageWrapper> images = new ArrayList<>();
        for (DatasetWrapper dataset : datasets) {
            images.addAll(dataset.getImages(client));
        }
        images.sort(Comparator.comparing(ImageWrapper::getId));

        return purge(images);
    }


    /**
     * Gets all images in the project with a certain from OMERO.
     *
     * @param client The client handling the connection.
     * @param name   Name searched.
     *
     * @return ImageWrapper list.
     *
     * @throws ServiceException   Cannot connect to OMERO.
     * @throws AccessException    Cannot access data.
     * @throws ExecutionException A Facility can't be retrieved or instantiated.
     */
    public List<ImageWrapper> getImages(Client client, String name)
    throws ServiceException, AccessException, ExecutionException {
        Collection<DatasetWrapper> datasets = getDatasets();

        List<ImageWrapper> imageWrappers = new ArrayList<>();
        for (DatasetWrapper dataset : datasets) {
            imageWrappers.addAll(dataset.getImages(client, name));
        }
        imageWrappers.sort(Comparator.comparing(ImageWrapper::getId));

        return purge(imageWrappers);
    }


    /**
     * Gets all images in the project with a certain motif in their name from OMERO.
     *
     * @param client The client handling the connection.
     * @param motif  Motif searched in an image name.
     *
     * @return ImageWrapper list.
     *
     * @throws ServiceException   Cannot connect to OMERO.
     * @throws AccessException    Cannot access data.
     * @throws ExecutionException A Facility can't be retrieved or instantiated.
     */
    public List<ImageWrapper> getImagesLike(Client client, String motif)
    throws ServiceException, AccessException, ExecutionException {
        List<ImageWrapper> images = new ArrayList<>();
        for (DatasetWrapper dataset : getDatasets()) {
            images.addAll(dataset.getImagesLike(client, motif));
        }
        images.sort(Comparator.comparing(ImageWrapper::getId));

        return purge(images);
    }


    /**
     * Gets all images in the project tagged with a specified tag from OMERO.
     *
     * @param client The client handling the connection.
     * @param tag    TagAnnotationWrapper containing the tag researched.
     *
     * @return ImageWrapper list.
     *
     * @throws ServiceException   Cannot connect to OMERO.
     * @throws AccessException    Cannot access data.
     * @throws OMEROServerError   Server error.
     * @throws ExecutionException A Facility can't be retrieved or instantiated.
     */
    public List<ImageWrapper> getImages(Client client, TagAnnotationWrapper tag)
    throws ServiceException, AccessException, OMEROServerError, ExecutionException {
        List<ImageWrapper> images = new ArrayList<>();
        for (DatasetWrapper dataset : getDatasets()) {
            images.addAll(dataset.getImages(client, tag));
        }
        images.sort(Comparator.comparing(ImageWrapper::getId));

        return purge(images);
    }


    /**
     * Gets all images in the project tagged with a specified tag from OMERO.
     *
     * @param client The client handling the connection.
     * @param tagId  Id of the tag researched.
     *
     * @return ImageWrapper list.
     *
     * @throws ServiceException   Cannot connect to OMERO.
     * @throws AccessException    Cannot access data.
     * @throws OMEROServerError   Server error.
     * @throws ExecutionException A Facility can't be retrieved or instantiated.
     */
    public List<ImageWrapper> getImagesTagged(Client client, Long tagId)
    throws ServiceException, AccessException, OMEROServerError, ExecutionException {
        List<ImageWrapper> images = new ArrayList<>();
        for (DatasetWrapper dataset : getDatasets()) {
            images.addAll(dataset.getImagesTagged(client, tagId));
        }
        images.sort(Comparator.comparing(ImageWrapper::getId));

        return purge(images);
    }


    /**
     * Gets all images in the project with a certain key
     *
     * @param client The client handling the connection.
     * @param key    Name of the key researched.
     *
     * @return ImageWrapper list.
     *
     * @throws ServiceException   Cannot connect to OMERO.
     * @throws AccessException    Cannot access data.
     * @throws ExecutionException A Facility can't be retrieved or instantiated.
     */
    public List<ImageWrapper> getImagesWithKey(Client client, String key)
    throws ServiceException, AccessException, ExecutionException {
        List<ImageWrapper> images = new ArrayList<>();
        for (DatasetWrapper dataset : getDatasets()) {
            images.addAll(dataset.getImagesWithKey(client, key));
        }
        images.sort(Comparator.comparing(ImageWrapper::getId));

        return purge(images);
    }


    /**
     * Gets all images in the project with a certain key value pair from OMERO.
     *
     * @param client The client handling the connection.
     * @param key    Name of the key researched.
     * @param value  Value associated with the key.
     *
     * @return ImageWrapper list.
     *
     * @throws ServiceException   Cannot connect to OMERO.
     * @throws AccessException    Cannot access data.
     * @throws ExecutionException A Facility can't be retrieved or instantiated.
     */
    public List<ImageWrapper> getImages(Client client, String key, String value)
    throws ServiceException, AccessException, ExecutionException {
        List<ImageWrapper> images = new ArrayList<>();
        for (DatasetWrapper dataset : getDatasets()) {
            images.addAll(dataset.getImages(client, key, value));
        }
        images.sort(Comparator.comparing(ImageWrapper::getId));

        return purge(images);
    }


    /**
     * Refreshes the wrapped project.
     *
     * @param client The client handling the connection.
     *
     * @throws ServiceException   Cannot connect to OMERO.
     * @throws AccessException    Cannot access data.
     * @throws ExecutionException A Facility can't be retrieved or instantiated.
     */
    public void refresh(Client client) throws ServiceException, AccessException, ExecutionException {
        data = handleServiceAndAccess(client.getBrowseFacility(),
                                      bf -> bf.getProjects(client.getCtx(),
                                                           Collections.singletonList(this.getId()))
                                              .iterator().next(),
                                      "Cannot refresh " + this);
    }

}