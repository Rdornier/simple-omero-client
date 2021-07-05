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

package fr.igred.omero.meta;


import fr.igred.omero.Client;
import fr.igred.omero.GenericObjectWrapper;
import fr.igred.omero.meta.GroupWrapper.SortByName;
import omero.gateway.model.ExperimenterData;
import omero.gateway.model.GroupData;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;


public class ExperimenterWrapper extends GenericObjectWrapper<ExperimenterData> {

    /**
     * Constructor of the class ExperimenterWrapper.
     *
     * @param client       The client handling the connection.
     * @param experimenter The experimenter contained in the ExperimenterWrapper.
     */
    public ExperimenterWrapper(Client client, ExperimenterData experimenter) {
        super(client, experimenter);
    }


    /**
     * @return ExperimenterData contained.
     */
    public ExperimenterData asExperimenterData() {
        return data;
    }


    /**
     * Returns the first name of the experimenter.
     *
     * @return see above.
     */
    public String getFirstName() {
        return data.getFirstName();
    }


    /**
     * Sets the first name of the experimenter.
     *
     * @param firstName The value to set.
     */
    public void setFirstName(String firstName) {
        data.setFirstName(firstName);
    }


    /**
     * Returns the last name of the experimenter.
     *
     * @return see above.
     */
    public String getLastName() {
        return data.getLastName();
    }


    /**
     * Sets the last name of the experimenter.
     *
     * @param lastName The value to set.
     */
    public void setLastName(String lastName) {
        data.setLastName(lastName);
    }


    /**
     * Returns the last name of the experimenter.
     *
     * @return see above.
     */
    public String getUserName() {
        return data.getUserName();
    }


    /**
     * Returns the e-mail of the experimenter.
     *
     * @return see above.
     */
    public String getEmail() {
        return data.getEmail();
    }


    /**
     * Sets the e-mail of the experimenter.
     *
     * @param email The value to set.
     */
    public void setEmail(String email) {
        data.setEmail(email);
    }


    /**
     * Returns the institution where the experimenter works.
     *
     * @return see above.
     */
    public String getInstitution() {
        return data.getInstitution();
    }


    /**
     * Sets the institution where the experimenter works.
     *
     * @param institution The value to set.
     */
    public void setInstitution(String institution) {
        data.setInstitution(institution);
    }


    /**
     * Returns the groups the experimenter is a member of.
     *
     * @return See above.
     */
    public List<GroupWrapper> getGroups() {
        List<GroupData> groups = data.getGroups();

        List<GroupWrapper> groupWrappers = new ArrayList<>(groups.size());
        for (GroupData group : groups) {
            groupWrappers.add(new GroupWrapper(client, group));
        }
        groupWrappers.sort(new SortByName<>());
        return groupWrappers;
    }


    /**
     * Returns the default Group for this Experimenter
     *
     * @return See above.
     */
    public GroupWrapper getDefaultGroup() {
        return new GroupWrapper(client, data.getDefaultGroup());
    }


    /**
     * Returns the middle name of the experimenter.
     *
     * @return see above.
     */
    public String getMiddleName() {
        return data.getMiddleName();
    }


    /**
     * Sets the middle name of the experimenter.
     *
     * @param middleName The value to set.
     */
    public void setMiddleName(String middleName) {
        data.setMiddleName(middleName);
    }


    /**
     * Returns <code>true</code> if the experimenter is active,
     * <code>false</code> otherwise.
     *
     * @return See above.
     */
    public boolean isActive() {
        return data.isActive();
    }


    /**
     * Checks if supplied group id matches any group to which the current experimenter belongs to.
     *
     * @param groupId The id of the group.
     *
     * @return boolean <code>true</code>/<code>false</code> depending if matching id found
     */
    public boolean isMemberOfGroup(long groupId) {
        return data.isMemberOfGroup(groupId);
    }


    /**
     * Returns <code>true</code> if the user is connected via LDAP.
     *
     * @return See above.
     */
    public boolean isLDAP() {
        return data.isLDAP();
    }


    /**
     * Class used to sort ExperimenterWrappers.
     */
    public static class SortByLastName<U extends ExperimenterWrapper> implements Comparator<U> {

        /**
         * Compare 2 ExperimenterWrappers. Compare the last names of the ExperimenterWrappers.
         *
         * @param object1 First object to compare.
         * @param object2 Second object to compare.
         *
         * @return <ul><li>-1 if the last name of object1 is lower than the id object2.</li>
         * <li>0  if the last names are the same.</li>
         * <li>1 if the last name of object1 is greater than the id of object2.</li></ul>
         */
        public int compare(U object1, U object2) {
            return object1.getLastName().compareTo(object2.getLastName());
        }

    }

}
