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

package fr.igred.omero;


import fr.igred.omero.annotations.TableWrapper;
import fr.igred.omero.annotations.TagAnnotationWrapper;
import fr.igred.omero.exception.AccessException;
import fr.igred.omero.exception.ExceptionHandler;
import fr.igred.omero.exception.ServerException;
import fr.igred.omero.exception.ServiceException;
import fr.igred.omero.meta.ExperimenterWrapper;
import fr.igred.omero.meta.GroupWrapper;
import fr.igred.omero.repository.DatasetWrapper;
import fr.igred.omero.repository.FolderWrapper;
import fr.igred.omero.repository.ImageWrapper;
import fr.igred.omero.repository.ProjectWrapper;
import ome.formats.OMEROMetadataStoreClient;
import omero.RLong;
import omero.ServerError;
import omero.gateway.Gateway;
import omero.gateway.JoinSessionCredentials;
import omero.gateway.LoginCredentials;
import omero.gateway.SecurityContext;
import omero.gateway.exception.DSAccessException;
import omero.gateway.exception.DSOutOfServiceException;
import omero.gateway.facility.AdminFacility;
import omero.gateway.facility.BrowseFacility;
import omero.gateway.facility.DataManagerFacility;
import omero.gateway.facility.MetadataFacility;
import omero.gateway.facility.ROIFacility;
import omero.gateway.facility.TablesFacility;
import omero.gateway.model.DatasetData;
import omero.gateway.model.ExperimenterData;
import omero.gateway.model.GroupData;
import omero.gateway.model.ImageData;
import omero.gateway.model.ProjectData;
import omero.gateway.model.TagAnnotationData;
import omero.log.SimpleLogger;
import omero.model.FileAnnotationI;
import omero.model.IObject;
import omero.model.TagAnnotation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static fr.igred.omero.exception.ExceptionHandler.handleServiceAndAccess;
import static fr.igred.omero.exception.ExceptionHandler.handleServiceAndServer;


/**
 * Basic class, contains the gateway, the security context, and multiple facilities.
 * <p>
 * Allows the user to connect to OMERO and browse through all the data accessible to the user.
 */
public class Client {

    /** User */
    private ExperimenterWrapper user;

    /** Gateway linking the code to OMERO, only linked to one group. */
    private Gateway gateway;

    /** Security context of the user, contains the permissions of the user in this group. */
    private SecurityContext ctx;


    /**
     * Constructor of the Client class. Initializes the gateway.
     */
    public Client() {
        gateway = new Gateway(new SimpleLogger());
        user = null;
        ctx = null;
    }


    /**
     * Returns the current user.
     *
     * @return The current user.
     */
    public ExperimenterWrapper getUser() {
        return user;
    }


    /**
     * Contains the permissions of the user in the group.
     *
     * @return the {@link SecurityContext} of the user.
     */
    public SecurityContext getCtx() {
        return ctx;
    }


    /**
     * Gets the BrowseFacility used to access the data from OMERO.
     *
     * @return the {@link BrowseFacility} linked to the gateway.
     *
     * @throws ExecutionException A Facility can't be retrieved or instantiated.
     */
    public BrowseFacility getBrowseFacility() throws ExecutionException {
        return gateway.getFacility(BrowseFacility.class);
    }


    /**
     * Gets the DataManagerFacility to handle/write data on OMERO. A
     *
     * @return the {@link DataManagerFacility} linked to the gateway.
     *
     * @throws ExecutionException If the DataManagerFacility can't be retrieved or instantiated.
     */
    public DataManagerFacility getDm() throws ExecutionException {
        return gateway.getFacility(DataManagerFacility.class);
    }


    /**
     * Gets the MetadataFacility used to manipulate annotations from OMERO.
     *
     * @return the {@link MetadataFacility} linked to the gateway.
     *
     * @throws ExecutionException If the MetadataFacility can't be retrieved or instantiated.
     */
    public MetadataFacility getMetadata() throws ExecutionException {
        return gateway.getFacility(MetadataFacility.class);
    }


    /**
     * Gets the ROIFacility used to manipulate ROI from OMERO.
     *
     * @return the {@link ROIFacility} linked to the gateway.
     *
     * @throws ExecutionException If the ROIFacility can't be retrieved or instantiated.
     */
    public ROIFacility getRoiFacility() throws ExecutionException {
        return gateway.getFacility(ROIFacility.class);
    }


    /**
     * Gets the TablesFacility used to manipulate table from OMERO.
     *
     * @return the {@link TablesFacility} linked to the gateway.
     *
     * @throws ExecutionException If the TablesFacility can't be retrieved or instantiated.
     */
    public TablesFacility getTablesFacility() throws ExecutionException {
        return gateway.getFacility(TablesFacility.class);
    }


    /**
     * Gets the AdminFacility linked to the gateway to use admin specific function.
     *
     * @return the {@link AdminFacility} linked to the gateway.
     *
     * @throws ExecutionException If the AdminFacility can't be retrieved or instantiated.
     */
    public AdminFacility getAdminFacility() throws ExecutionException {
        return gateway.getFacility(AdminFacility.class);
    }


    /**
     * Finds objects on OMERO through a database query.
     *
     * @param query The database query.
     *
     * @return A list of OMERO objects.
     *
     * @throws ServiceException Cannot connect to OMERO.
     * @throws ServerException Server error.
     */
    public List<IObject> findByQuery(String query) throws ServiceException, ServerException {
        String error = "Query failed: " + query;
        return handleServiceAndServer(gateway,
                                      g -> g.getQueryService(ctx).findAllByQuery(query, null),
                                      error);
    }


    /**
     * Creates or recycles the import store.
     *
     * @return config.
     *
     * @throws ServiceException Cannot connect to OMERO.
     */
    public OMEROMetadataStoreClient getImportStore() throws ServiceException {
        OMEROMetadataStoreClient store;
        try {
            store = gateway.getImportStore(ctx);
        } catch (DSOutOfServiceException e) {
            throw new ServiceException("Could not retrieve import store", e, e.getConnectionStatus());
        }
        return store;
    }


    /**
     * Gets the user id.
     *
     * @return The user ID.
     */
    public long getId() {
        return user.getId();
    }


    /**
     * Gets the current group ID.
     *
     * @return The group ID.
     */
    public long getCurrentGroupId() {
        return ctx.getGroupID();
    }


    /**
     * Get the ID of the current session
     *
     * @return See above
     *
     * @throws ServiceException If the connection is broken, or not logged in
     */
    public String getSessionId() throws ServiceException {
        String sessionId;
        try {
            sessionId = gateway.getSessionId(user.asExperimenterData());
        } catch (DSOutOfServiceException e) {
            throw new ServiceException("Could not retrieve session ID", e, e.getConnectionStatus());
        }
        return sessionId;
    }


    public Gateway getGateway() {
        return gateway;
    }


    /**
     * Connects to OMERO using a session ID.
     *
     * @param hostname  Name of the host.
     * @param port      Port used by OMERO.
     * @param sessionId The session ID.
     *
     * @throws ServiceException Cannot connect to OMERO.
     */
    public void connect(String hostname, int port, String sessionId)
    throws ServiceException {
        connect(new JoinSessionCredentials(sessionId, hostname, port));
    }


    /**
     * Connects the user to OMERO.
     * <p> Uses the argument to connect to the gateway.
     * <p> Connects to the group specified in the argument.
     *
     * @param hostname Name of the host.
     * @param port     Port used by OMERO.
     * @param username Username of the user.
     * @param password Password of the user.
     * @param groupID  ID of the group to connect.
     *
     * @throws ServiceException Cannot connect to OMERO.
     */
    public void connect(String hostname, int port, String username, char[] password, Long groupID)
    throws ServiceException {
        LoginCredentials cred = new LoginCredentials(username, String.valueOf(password), hostname, port);
        cred.setGroupID(groupID);
        connect(cred);
    }


    /**
     * Connects the user to OMERO.
     * <p> Uses the argument to connect to the gateway.
     * <p> Connects to the default group of the user.
     *
     * @param hostname Name of the host.
     * @param port     Port used by OMERO.
     * @param username Username of the user.
     * @param password Password of the user.
     *
     * @throws ServiceException Cannot connect to OMERO.
     */
    public void connect(String hostname, int port, String username, char[] password)
    throws ServiceException {
        connect(new LoginCredentials(username, String.valueOf(password), hostname, port));
    }


    /**
     * Connects the user to OMERO. Gets the SecurityContext and the BrowseFacility.
     *
     * @param cred User credential.
     *
     * @throws ServiceException Cannot connect to OMERO.
     */
    public void connect(LoginCredentials cred) throws ServiceException {
        disconnect();

        try {
            this.user = new ExperimenterWrapper(gateway.connect(cred));
        } catch (DSOutOfServiceException oos) {
            throw new ServiceException(oos, oos.getConnectionStatus());
        }
        this.ctx = new SecurityContext(user.getGroupId());
        this.ctx.setExperimenter(this.user.asExperimenterData());
    }


    /**
     * Disconnects the user
     */
    public void disconnect() {
        if (gateway.isConnected()) {
            if (ctx != null) {
                ctx.setExperimenter(null);
            }
            ctx = null;
            user = null;
            gateway.disconnect();
        }
    }


    /**
     * Gets the project with the specified id from OMERO.
     *
     * @param ids Projects IDs
     *
     * @return ProjectWrapper containing the project.
     *
     * @throws ServiceException   Cannot connect to OMERO.
     * @throws AccessException    Cannot access data.
     * @throws ExecutionException A Facility can't be retrieved or instantiated.
     */
    public List<ProjectWrapper> getProjects(Long... ids)
    throws ServiceException, AccessException, ExecutionException {
        String error = "Cannot get projects with IDs: " + Arrays.toString(ids);
        Collection<ProjectData> projects = handleServiceAndAccess(getBrowseFacility(),
                                                                  bf -> bf.getProjects(ctx, Arrays.asList(ids)),
                                                                  error);
        return ObjectWrapper.wrap(projects, ProjectWrapper::new, ProjectWrapper::getId);
    }


    /**
     * Gets all projects available from OMERO.
     *
     * @return Collection of ProjectWrapper.
     *
     * @throws ServiceException   Cannot connect to OMERO.
     * @throws AccessException    Cannot access data.
     * @throws ExecutionException A Facility can't be retrieved or instantiated.
     */
    public List<ProjectWrapper> getProjects() throws ServiceException, AccessException, ExecutionException {
        Collection<ProjectData> projects = handleServiceAndAccess(getBrowseFacility(),
                                                                  bf -> bf.getProjects(ctx),
                                                                  "Cannot get projects");
        return ObjectWrapper.wrap(projects, ProjectWrapper::new, ProjectWrapper::getId);
    }


    /**
     * Gets all projects with a certain name from OMERO.
     *
     * @param name Name searched.
     *
     * @return Collection of ProjectWrapper.
     *
     * @throws ServiceException   Cannot connect to OMERO.
     * @throws AccessException    Cannot access data.
     * @throws ExecutionException A Facility can't be retrieved or instantiated.
     */
    public List<ProjectWrapper> getProjects(String name) throws ServiceException, AccessException, ExecutionException {
        String error = "Cannot get projects with name: " + name;
        Collection<ProjectData> projects = handleServiceAndAccess(getBrowseFacility(),
                                                                  bf -> bf.getProjects(ctx, name),
                                                                  error);
        return ObjectWrapper.wrap(projects, ProjectWrapper::new, ProjectWrapper::getId);
    }


    /**
     * Gets the dataset with the specified id from OMERO.
     *
     * @param ids Datasets IDs.
     *
     * @return ProjectWrapper containing the project.
     *
     * @throws ServiceException   Cannot connect to OMERO.
     * @throws AccessException    Cannot access data.
     * @throws ExecutionException A Facility can't be retrieved or instantiated.
     */
    public List<DatasetWrapper> getDatasets(Long... ids)
    throws ServiceException, AccessException, ExecutionException {
        String error = "Cannot get dataset with ID: " + Arrays.toString(ids);
        Collection<DatasetData> datasets = handleServiceAndAccess(getBrowseFacility(),
                                                                  bf -> bf.getDatasets(ctx, Arrays.asList(ids)),
                                                                  error);
        return ObjectWrapper.wrap(datasets, DatasetWrapper::new, DatasetWrapper::getId);
    }


    /**
     * Gets all the datasets available from OMERO.
     *
     * @return Collection of DatasetWrapper.
     *
     * @throws ServiceException   Cannot connect to OMERO.
     * @throws AccessException    Cannot access data.
     * @throws ServerException   Server error.
     * @throws ExecutionException A Facility can't be retrieved or instantiated.
     */
    public List<DatasetWrapper> getDatasets()
    throws ServiceException, AccessException, ServerException, ExecutionException {
        List<IObject>        os       = findByQuery("select d from Dataset d");
        Long[]               ids      = os.stream().map(IObject::getId).map(RLong::getValue).toArray(Long[]::new);
        List<DatasetWrapper> datasets = getDatasets(ids);
        datasets.sort(Comparator.comparing(DatasetWrapper::getId));
        return datasets;
    }


    /**
     * Gets all datasets with a certain name from OMERO.
     *
     * @param name Name searched.
     *
     * @return Collection of DatasetWrapper.
     *
     * @throws ServiceException   Cannot connect to OMERO.
     * @throws AccessException    Cannot access data.
     * @throws ExecutionException A Facility can't be retrieved or instantiated.
     */
    public List<DatasetWrapper> getDatasets(String name) throws ServiceException, AccessException, ExecutionException {
        String error = "Cannot get datasets with name: " + name;
        Collection<DatasetData> datasets = handleServiceAndAccess(getBrowseFacility(),
                                                                  bf -> bf.getDatasets(ctx, name),
                                                                  error);
        return ObjectWrapper.wrap(datasets, DatasetWrapper::new, DatasetWrapper::getId);
    }


    /**
     * Returns an ImageWrapper that contains the image with the specified id from OMERO.
     *
     * @param ids Images IDs.
     *
     * @return ImageWrapper containing the image.
     *
     * @throws ServiceException   Cannot connect to OMERO.
     * @throws AccessException    Cannot access data.
     * @throws ExecutionException A Facility can't be retrieved or instantiated.
     */
    public List<ImageWrapper> getImages(Long... ids)
    throws ServiceException, AccessException, ExecutionException {
        String error = "Cannot get image with ID: " + Arrays.toString(ids);
        Collection<ImageData> images = handleServiceAndAccess(getBrowseFacility(),
                                                              bf -> bf.getImages(ctx, Arrays.asList(ids)),
                                                              error);
        return ObjectWrapper.wrap(images, ImageWrapper::new, ImageWrapper::getId);
    }


    /**
     * Gets the user images available from OMERO.
     *
     * @return ImageWrapper list.
     *
     * @throws ServiceException   Cannot connect to OMERO.
     * @throws AccessException    Cannot access data.
     * @throws ExecutionException A Facility can't be retrieved or instantiated.
     */
    public List<ImageWrapper> getUserImages() throws ServiceException, AccessException, ExecutionException {
        Collection<ImageData> images = handleServiceAndAccess(getBrowseFacility(),
                                                              bf -> bf.getUserImages(ctx),
                                                              "Cannot get images");
        return ObjectWrapper.wrap(images, ImageWrapper::new, ImageWrapper::getId);
    }


    /**
     * Gets all images with a certain from OMERO.
     *
     * @param name Name searched.
     *
     * @return ImageWrapper list.
     *
     * @throws ServiceException   Cannot connect to OMERO.
     * @throws AccessException    Cannot access data.
     * @throws ExecutionException A Facility can't be retrieved or instantiated.
     */
    public List<ImageWrapper> getImages(String name) throws ServiceException, AccessException, ExecutionException {
        String error = "Cannot get images with name: " + name;
        Collection<ImageData> images = handleServiceAndAccess(getBrowseFacility(),
                                                              bf -> bf.getImages(ctx, name),
                                                              error);
        images.removeIf(image -> !image.getName().equals(name));
        return ObjectWrapper.wrap(images, ImageWrapper::new, ImageWrapper::getId);
    }


    /**
     * Gets all images with a certain motif in their name from OMERO.
     *
     * @param motif Motif searched in an image name.
     *
     * @return ImageWrapper list.
     *
     * @throws ServiceException   Cannot connect to OMERO.
     * @throws AccessException    Cannot access data.
     * @throws ExecutionException A Facility can't be retrieved or instantiated.
     */
    public List<ImageWrapper> getImagesLike(String motif) throws ServiceException, AccessException, ExecutionException {
        List<ImageWrapper> images = getUserImages();
        String             regexp = ".*" + motif + ".*";
        images.removeIf(image -> !image.getName().matches(regexp));
        return images;
    }


    /**
     * Gets all images tagged with a specified tag from OMERO.
     *
     * @param tag TagAnnotationWrapper containing the tag researched.
     *
     * @return ImageWrapper list.
     *
     * @throws ServiceException   Cannot connect to OMERO.
     * @throws AccessException    Cannot access data.
     * @throws ServerException   Server error.
     * @throws ExecutionException A Facility can't be retrieved or instantiated.
     */
    public List<ImageWrapper> getImages(TagAnnotationWrapper tag)
    throws ServiceException, AccessException, ServerException, ExecutionException {
        return tag.getImages(this);
    }


    /**
     * Gets all images tagged with a specified tag from OMERO.
     *
     * @param tagId Id of the tag researched.
     *
     * @return ImageWrapper list.
     *
     * @throws ServiceException   Cannot connect to OMERO.
     * @throws AccessException    Cannot access data.
     * @throws ServerException   Server error.
     * @throws ExecutionException A Facility can't be retrieved or instantiated.
     */
    public List<ImageWrapper> getImagesTagged(Long tagId)
    throws ServiceException, AccessException, ServerException, ExecutionException {
        return getTag(tagId).getImages(this);
    }


    /**
     * Gets all images with a certain key
     *
     * @param key Name of the key researched.
     *
     * @return ImageWrapper list.
     *
     * @throws ServiceException   Cannot connect to OMERO.
     * @throws AccessException    Cannot access data.
     * @throws ExecutionException A Facility can't be retrieved or instantiated.
     */
    public List<ImageWrapper> getImagesWithKey(String key)
    throws ServiceException, AccessException, ExecutionException {
        List<ImageWrapper> images   = getUserImages();
        List<ImageWrapper> selected = new ArrayList<>(images.size());
        for (ImageWrapper image : images) {
            Map<String, String> pairsKeyValue = image.getKeyValuePairs(this);
            if (pairsKeyValue.get(key) != null) {
                selected.add(image);
            }
        }

        return selected;
    }


    /**
     * Gets all images with a certain key value pair from OMERO
     *
     * @param key   Name of the key researched.
     * @param value Value associated with the key.
     *
     * @return ImageWrapper list.
     *
     * @throws ServiceException   Cannot connect to OMERO.
     * @throws AccessException    Cannot access data.
     * @throws ExecutionException A Facility can't be retrieved or instantiated.
     */
    public List<ImageWrapper> getImages(String key, String value)
    throws ServiceException, AccessException, ExecutionException {
        List<ImageWrapper> images   = getUserImages();
        List<ImageWrapper> selected = new ArrayList<>(images.size());
        for (ImageWrapper image : images) {
            Map<String, String> pairsKeyValue = image.getKeyValuePairs(this);
            if (pairsKeyValue.get(key) != null && pairsKeyValue.get(key).equals(value)) {
                selected.add(image);
            }
        }

        return selected;
    }


    /**
     * Gets the client associated with the username in the parameters. The user calling this function needs to have
     * administrator rights. All action realized with the client returned will be considered as his.
     *
     * @param username Username of user.
     *
     * @return The client corresponding to the new user.
     *
     * @throws ServiceException       Cannot connect to OMERO.
     * @throws AccessException        Cannot access data.
     * @throws ExecutionException     A Facility can't be retrieved or instantiated.
     * @throws NoSuchElementException The requested user does not exist.
     */
    public Client sudo(String username) throws ServiceException, AccessException, ExecutionException {
        Client c = new Client();

        c.gateway = this.gateway;
        c.user = getUser(username);

        c.ctx = new SecurityContext(c.user.getDefaultGroup().getId());
        c.ctx.setExperimenter(c.user.asExperimenterData());
        c.ctx.sudo();

        return c;
    }


    /**
     * Change the current group used by the current user;
     *
     * @param groupId The group ID.
     */
    public void switchGroup(long groupId) {
        boolean sudo = ctx.isSudo();
        ctx = new SecurityContext(groupId);
        ctx.setExperimenter(user.asExperimenterData());
        if (sudo) ctx.sudo();
    }


    /**
     * Gets the list of TagAnnotationWrapper available to the user
     *
     * @return list of TagAnnotationWrapper.
     *
     * @throws ServerException Server error.
     * @throws ServiceException Cannot connect to OMERO.
     */
    public List<TagAnnotationWrapper> getTags() throws ServerException, ServiceException {
        List<IObject> os = handleServiceAndServer(gateway,
                                                  g -> g.getQueryService(ctx)
                                                        .findAll(TagAnnotation.class.getSimpleName(),
                                                                 null),
                                                  "Cannot get tags");
        return os.stream()
                 .map(TagAnnotation.class::cast)
                 .map(TagAnnotationData::new)
                 .map(TagAnnotationWrapper::new)
                 .sorted(Comparator.comparing(TagAnnotationWrapper::getId))
                 .collect(Collectors.toList());
    }


    /**
     * Gets the list of TagAnnotationWrapper with the specified name available to the user
     *
     * @param name Name of the tag searched.
     *
     * @return list of TagAnnotationWrapper.
     *
     * @throws ServerException Server error.
     * @throws ServiceException Cannot connect to OMERO.
     */
    public List<TagAnnotationWrapper> getTags(String name) throws ServerException, ServiceException {
        List<TagAnnotationWrapper> tags = getTags();
        tags.removeIf(tag -> !tag.getName().equals(name));
        tags.sort(Comparator.comparing(TagAnnotationWrapper::getId));
        return tags;
    }


    /**
     * Gets a specific tag from the OMERO database
     *
     * @param id Id of the tag.
     *
     * @return TagAnnotationWrapper containing the specified tag.
     *
     * @throws ServerException Server error.
     * @throws ServiceException Cannot connect to OMERO.
     */
    public TagAnnotationWrapper getTag(Long id) throws ServerException, ServiceException {
        IObject o = handleServiceAndServer(gateway,
                                           g -> g.getQueryService(ctx)
                                                 .find(TagAnnotation.class.getSimpleName(),
                                                       id),
                                           "Cannot get tag ID: " + id);
        TagAnnotationData tag = new TagAnnotationData((TagAnnotation) Objects.requireNonNull(o));
        tag.setNameSpace(tag.getContentAsString());
        return new TagAnnotationWrapper(tag);
    }


    /**
     * Saves an object on OMERO.
     *
     * @param object The OMERO object.
     *
     * @return The saved OMERO object
     *
     * @throws ServiceException   Cannot connect to OMERO.
     * @throws AccessException    Cannot access data.
     * @throws ExecutionException A Facility can't be retrieved or instantiated.
     */
    public IObject save(IObject object) throws ServiceException, AccessException, ExecutionException {
        return handleServiceAndAccess(getDm(),
                                      d -> d.saveAndReturnObject(ctx, object),
                                      "Cannot save object");
    }


    /**
     * Deletes an object from OMERO.
     *
     * @param object The OMERO object.
     *
     * @throws ServiceException     Cannot connect to OMERO.
     * @throws AccessException      Cannot access data.
     * @throws ExecutionException   A Facility can't be retrieved or instantiated.
     * @throws ServerException     If the thread was interrupted.
     * @throws InterruptedException If block(long) does not return.
     */
    void delete(IObject object)
    throws ServiceException, AccessException, ExecutionException, ServerException, InterruptedException {
        final long wait = 500L;
        ExceptionHandler.of(getDm(), "Cannot delete object")
                        .map(d -> d.delete(ctx, object).loop(10, wait))
                        .propagate(InterruptedException.class)
                        .propagate(DSOutOfServiceException.class, ServiceException::new)
                        .propagate(DSAccessException.class, AccessException::new)
                        .propagate(ServerError.class, ServerException::new);
    }


    /**
     * Deletes an object from OMERO.
     *
     * @param object The OMERO object.
     *
     * @throws ServiceException     Cannot connect to OMERO.
     * @throws AccessException      Cannot access data.
     * @throws ExecutionException   A Facility can't be retrieved or instantiated.
     * @throws ServerException     If the thread was interrupted.
     * @throws InterruptedException If block(long) does not return.
     */
    public void delete(ObjectWrapper<?> object)
    throws ServiceException, AccessException, ExecutionException, ServerException, InterruptedException {
        if (object instanceof FolderWrapper) {
            ((FolderWrapper) object).unlinkAllROI(this);
        }
        delete(object.asIObject());
    }


    /**
     * Deletes a table from OMERO
     *
     * @param table TableWrapper containing the table to delete.
     *
     * @throws ServiceException         Cannot connect to OMERO.
     * @throws AccessException          Cannot access data.
     * @throws ExecutionException       A Facility can't be retrieved or instantiated.
     * @throws IllegalArgumentException Id not defined.
     * @throws ServerException         If the thread was interrupted.
     * @throws InterruptedException     If block(long) does not return.
     */
    public void delete(TableWrapper table)
    throws ServiceException, AccessException, ExecutionException, ServerException, InterruptedException {
        deleteFile(table.getId());
    }


    /**
     * Deletes a file from OMERO
     *
     * @param id ID of the file to delete.
     *
     * @throws ServiceException     Cannot connect to OMERO.
     * @throws AccessException      Cannot access data.
     * @throws ExecutionException   A Facility can't be retrieved or instantiated.
     * @throws ServerException     If the thread was interrupted.
     * @throws InterruptedException If block(long) does not return.
     */
    public void deleteFile(Long id)
    throws ServiceException, AccessException, ExecutionException, ServerException, InterruptedException {
        FileAnnotationI file = new FileAnnotationI(id, false);
        delete(file);
    }


    /**
     * Returns the user which matches the username.
     *
     * @param username The name of the user.
     *
     * @return The user matching the username, or null if it does not exist.
     *
     * @throws ServiceException       Cannot connect to OMERO.
     * @throws AccessException        Cannot access data.
     * @throws ExecutionException     A Facility can't be retrieved or instantiated.
     * @throws NoSuchElementException The requested user does not exist.
     */
    public ExperimenterWrapper getUser(String username)
    throws ExecutionException, ServiceException, AccessException {
        ExperimenterData experimenter = handleServiceAndAccess(getAdminFacility(),
                                                               a -> a.lookupExperimenter(ctx, username),
                                                               "Cannot retrieve user: " + username);
        if (experimenter != null) {
            return new ExperimenterWrapper(experimenter);
        } else {
            throw new NoSuchElementException(String.format("User not found: %s", username));
        }
    }


    /**
     * Returns the group which matches the name.
     *
     * @param groupName The name of the group.
     *
     * @return The group with the appropriate name, or null if it does not exist.
     *
     * @throws ServiceException   Cannot connect to OMERO.
     * @throws AccessException    Cannot access data.
     * @throws ExecutionException A Facility can't be retrieved or instantiated.
     */
    public GroupWrapper getGroup(String groupName)
    throws ExecutionException, ServiceException, AccessException {
        GroupData group = handleServiceAndAccess(getAdminFacility(),
                                                 a -> a.lookupGroup(ctx, groupName),
                                                 "Cannot retrieve group: " + groupName);
        if (group != null) {
            return new GroupWrapper(group);
        } else {
            return null;
        }
    }


    /**
     * Check if the client is still connected to the server
     *
     * @return See above.
     */
    public boolean isConnected() {
        return gateway.isConnected() && ctx != null;
    }

}

