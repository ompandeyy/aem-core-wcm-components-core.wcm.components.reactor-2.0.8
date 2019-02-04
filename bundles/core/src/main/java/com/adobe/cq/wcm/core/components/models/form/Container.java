/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2017 Adobe Systems Incorporated
 ~
 ~ Licensed under the Apache License, Version 2.0 (the "License");
 ~ you may not use this file except in compliance with the License.
 ~ You may obtain a copy of the License at
 ~
 ~     http://www.apache.org/licenses/LICENSE-2.0
 ~
 ~ Unless required by applicable law or agreed to in writing, software
 ~ distributed under the License is distributed on an "AS IS" BASIS,
 ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 ~ See the License for the specific language governing permissions and
 ~ limitations under the License.
 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/
package com.adobe.cq.wcm.core.components.models.form;

import java.util.Map;

import javax.annotation.Nonnull;

import org.osgi.annotation.versioning.ConsumerType;

import com.adobe.cq.export.json.ComponentExporter;
import com.adobe.cq.export.json.ContainerExporter;

/**
 * Defines the form {@code Container} Sling Model used for the {@code /apps/core/wcm/components/form/container} component.
 *
 * @since com.adobe.cq.wcm.core.components.models.form 13.0.0
 */
@ConsumerType
public interface Container extends ContainerExporter {

    /**
     * Returns the form's submit method (the value of the form's HTML <code>method</code> attribute).
     *
     * @return form submit method (method attribute of form)
     * @since com.adobe.cq.wcm.core.components.models.form 13.0.0; marked <code>default</code> in 14.1.0
     */
    default String getMethod() {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns the form's submit method (the value of the form's HTML <code>action</code> attribute).
     *
     * @return form submit action (used in action attribute of form)
     * @since com.adobe.cq.wcm.core.components.models.form 13.0.0; marked <code>default</code> in 14.1.0
     */
    default String getAction() {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns the form's id (the value of the form's HTML <code>id</code> attribute).
     *
     * @return form id (used in id attribute of form)
     * @since com.adobe.cq.wcm.core.components.models.form 13.0.0; marked <code>default</code> in 14.1.0
     */
    default String getId() {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns the form's name (the value of the form's HTML <code>name</code> attribute).
     *
     * @return form name (used in name attribute of form)
     * @since com.adobe.cq.wcm.core.components.models.form 13.0.0; marked <code>default</code> in 14.1.0
     */
    default String getName() {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns the form's encoding type (the value of the form's HTML <code>enctype</code> attribute).
     *
     * @return form data enctype (used in enctype attribute of form)
     * @since com.adobe.cq.wcm.core.components.models.form 13.0.0; marked <code>default</code> in 14.1.0
     */
    default String getEnctype() {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns the resource type for the "new" section in the form container where other input components will
     * be dropped.
     *
     * @return resource type for the "new" section in form container where other input components will
     * be dropped
     * @since com.adobe.cq.wcm.core.components.models.form 13.0.0; marked <code>default</code> in 14.1.0
     */
    default String getResourceTypeForDropArea() {
        throw new UnsupportedOperationException();
    }

    /**
     * This method returns the redirect url property of this form. If the current sling request has a non-blank context path, the context
     * path is prepended to the redirect url if the redirect is an absolute path starting with '/'. This method also appends ".html" to the
     * redirect path.
     * 
     * @return The form redirect url (used in the :redirect hidden input field of the form)
     * @since com.adobe.cq.wcm.core.components.models.form 13.0.0; marked <code>default</code> in 14.1.0
     */
    default String getRedirect() {
        throw new UnsupportedOperationException();
    }

    /**
     * @see ContainerExporter#getExportedItemsOrder()
     * @since com.adobe.cq.wcm.core.components.models.form 14.2.0
     */
    @Nonnull
    @Override
    default String[] getExportedItemsOrder() {
        throw new UnsupportedOperationException();
    }

    /**
     * @see ContainerExporter#getExportedItems()
     * @since com.adobe.cq.wcm.core.components.models.form 14.2.0
     */
    @Nonnull
    @Override
    default Map<String, ? extends ComponentExporter> getExportedItems() {
        throw new UnsupportedOperationException();
    }

    /**
     * @see ContainerExporter#getExportedType()
     * @since com.adobe.cq.wcm.core.components.models.form 14.2.0
     */
    @Nonnull
    @Override
    default String getExportedType() {
        throw new UnsupportedOperationException();
    }
}
