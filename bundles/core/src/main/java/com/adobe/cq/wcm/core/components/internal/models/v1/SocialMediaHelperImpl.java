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
package com.adobe.cq.wcm.core.components.internal.models.v1;

import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.PostConstruct;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.Exporter;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.commerce.api.CommerceException;
import com.adobe.cq.commerce.api.CommerceService;
import com.adobe.cq.commerce.api.CommerceSession;
import com.adobe.cq.commerce.api.PriceInfo;
import com.adobe.cq.commerce.api.Product;
import com.adobe.cq.commerce.common.CommerceHelper;
import com.adobe.cq.commerce.common.PriceFilter;
import com.adobe.cq.export.json.ComponentExporter;
import com.adobe.cq.export.json.ExporterConstants;
import com.adobe.cq.wcm.core.components.models.SocialMediaHelper;
import com.adobe.cq.xf.social.ExperienceFragmentSocialVariation;
import com.day.cq.commons.Externalizer;
import com.day.cq.commons.ImageResource;
import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.wcm.api.Page;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Helper class for page functionality related to page sharing by user on social media platforms.
 */
@Model(adaptables = SlingHttpServletRequest.class, adapters = {SocialMediaHelper.class, ComponentExporter.class}, resourceType =
        SocialMediaHelperImpl.RESOURCE_TYPE)
@Exporter(name = ExporterConstants.SLING_MODEL_EXPORTER_NAME, extensions = ExporterConstants.SLING_MODEL_EXTENSION)
public class SocialMediaHelperImpl implements SocialMediaHelper {

    static final String RESOURCE_TYPE = "core/wcm/components/sharing/v1/sharing";

    private static final Logger LOGGER = LoggerFactory.getLogger(SocialMediaHelperImpl.class);

    //Open Graph metadata property names
    static final String OG_TITLE = "og:title";
    static final String OG_URL = "og:url";
    static final String OG_TYPE = "og:type";
    static final String OG_SITE_NAME = "og:site_name";
    static final String OG_IMAGE = "og:image";
    static final String OG_DESCRIPTION = "og:description";
    static final String OG_PRODUCT_PRICE_AMOUNT = "product:price:amount";
    static final String OG_PRODUCT_PRICE_CURRENCY = "product:price:currency";

    @ScriptVariable
    private Page currentPage = null;

    @Self
    private SlingHttpServletRequest request = null;

    @ScriptVariable
    private SlingHttpServletResponse response = null;

    @SlingObject
    private ResourceResolver resourceResolver = null;

    @OSGiService
    private Externalizer externalizer = null;

    /**
     * lazy variable, use hasSharingComponent() method for accessing it
     */
    private Boolean hasSharingComponent;
    private boolean facebookEnabled;
    private boolean pinterestEnabled;
    private boolean socialMediaEnabled;
    private String variantPath;

    /**
     * Holds the metadata for a page.
     */
    private Map<String, String> metadata;

    //*************** WEB INTERFACE METHODS *******************

    @Override
    public boolean isFacebookEnabled() {
        return facebookEnabled;
    }

    @Override
    public boolean isPinterestEnabled() {
        return pinterestEnabled;
    }

    @Override
    public boolean isSocialMediaEnabled() {
        return socialMediaEnabled;
    }

    @Override
    @JsonProperty("hasFacebookSharing")
    public boolean hasFacebookSharing() {
        return facebookEnabled && hasSharingComponent();
    }

    @Override
    @JsonProperty("hasPinteresSharing")
    public boolean hasPinterestSharing() {
        return pinterestEnabled && hasSharingComponent();
    }

    @Override
    public Map<String, String> getMetadata() {
        if (metadata == null) {
            initMetadata();
        }
        return metadata;
    }

    @Nonnull
    @Override
    public String getExportedType() {
        return request.getResource().getResourceType();
    }

    //*************** IMPLEMENTATION *******************
    @PostConstruct
    private void initModel() throws Exception {
        ValueMap pageProperties = currentPage.getProperties();
        String[] socialMedia = pageProperties.get(PN_SOCIAL_MEDIA, String[].class);
        facebookEnabled = ArrayUtils.contains(socialMedia, PV_FACEBOOK);
        pinterestEnabled = ArrayUtils.contains(socialMedia, PV_PINTEREST);
        socialMediaEnabled = facebookEnabled || pinterestEnabled;
        variantPath = pageProperties.get(PN_VARIANT_PATH, String.class);
    }

    //Private accessor for hasSharingComponent field providing lazy initialization.
    private boolean hasSharingComponent() {
        if (hasSharingComponent == null) {
            hasSharingComponent = hasSharingComponent(currentPage.getContentResource());
        }
        return hasSharingComponent;
    }

    /**
     * Search a resource tree for sharing component starting from a given resource.
     *
     * @param resource the resource
     * @return {@code true} if the sharing vomponent was found, {@code false} otherwise
     */
    private boolean hasSharingComponent(final Resource resource) {
        if (resource.isResourceType(RESOURCE_TYPE))
            return true;

        for (Resource child : resource.getChildren())
            if (hasSharingComponent(child))
                return true;

        return false;
    }

    /**
     * Prepares Open Graph metadata for a page to be shared on social media services.
     */
    private void initMetadata() {
        metadata = new LinkedHashMap<>();
        if (socialMediaEnabled) {
            WebsiteMetadata websiteMetadata = createMetadataProvider();
            put(OG_TITLE, websiteMetadata.getTitle());
            put(OG_URL, websiteMetadata.getURL());
            put(OG_TYPE, websiteMetadata.getTypeName());
            put(OG_SITE_NAME, websiteMetadata.getSiteName());
            put(OG_IMAGE, websiteMetadata.getImage());
            put(OG_DESCRIPTION, websiteMetadata.getDescription());

            if (pinterestEnabled && websiteMetadata instanceof ProductMetadata) {
                ProductMetadata productMetadata = (ProductMetadata) websiteMetadata;
                put(OG_PRODUCT_PRICE_AMOUNT, productMetadata.getProductPriceAmount());
                put(OG_PRODUCT_PRICE_CURRENCY, productMetadata.getProductPriceCurrency());
            }
        }
    }

    /**
     * Put non-blank named values in metadata map.
     */
    private void put(String name, String value) {
        if (StringUtils.isNotBlank(value)) {
            metadata.put(name, value);
        }
    }

    /**
     * Instantiates the suitable metadata provider based on the contents of the current page.
     */
    private WebsiteMetadata createMetadataProvider() {
        Product product = CommerceHelper.findCurrentProduct(currentPage);
        ExperienceFragmentSocialVariation smVariant = findExperienceFragmentSocialVariation();
        if (product == null) {
            if (smVariant == null) {
                return new WebsiteMetadataProvider();
            } else {
                return new ExperienceFragmentWebsiteMetadataProvider(smVariant);
            }
        } else {
            if (smVariant == null) {
                return new ProductMetadataProvider(product);
            } else {
                return new ExperienceFragmentProductMetadataProvider(product, smVariant);
            }
        }
    }

    private ExperienceFragmentSocialVariation findExperienceFragmentSocialVariation() {
        Page variantPage = currentPage.getPageManager().getPage(variantPath);
        if (variantPage == null)
            return null;

        ExperienceFragmentSocialVariation socialVariant = variantPage.adaptTo(ExperienceFragmentSocialVariation.class);
        return socialVariant;
    }

    /**
     * Provides metadata based on the content of a generic webpage.
     */
    private interface WebsiteMetadata {
        enum Type {website, product}

        String getTitle();

        String getURL();

        Type getType();

        String getTypeName();

        String getImage();

        String getDescription();

        String getSiteName();
    }

    /**
     * Provides metadata based on the content of a product page.
     */
    private interface ProductMetadata extends WebsiteMetadata {
        String getProductPriceAmount();

        String getProductPriceCurrency();
    }

    private class WebsiteMetadataProvider implements WebsiteMetadata {
        private static final String PN_IMAGE_FILE_JCR_CONTENT = "image/file/" + JcrConstants.JCR_CONTENT;

        @Override
        public String getTitle() {
            String title = currentPage.getTitle();
            if (StringUtils.isBlank(title)) {
                title = currentPage.getName();
            }
            return title;
        }

        @Override
        public String getURL() {
            String pagePath = currentPage.getPath();
            String extension = request.getRequestPathInfo().getExtension();
            String url = externalizer.publishLink(resourceResolver, pagePath) + "." + extension;
            return url;
        }

        @Override
        public Type getType() {
            return Type.website;
        }

        @Override
        public String getTypeName() {
            return getType().name();
        }

        @Override
        public String getImage() {
            String image = getThumbnailUrl(currentPage, 800, 480);
            image = externalizer.publishLink(resourceResolver, image);
            return image;
        }

        private String getThumbnailUrl(Page page, int width, int height) {
            String ck = "";

            ValueMap metadata = page.getProperties(PN_IMAGE_FILE_JCR_CONTENT);
            if (metadata != null) {
                Calendar imageLastModified = metadata.get(JcrConstants.JCR_LASTMODIFIED, Calendar.class);
                Calendar pageLastModified = page.getLastModified();
                if (pageLastModified != null && pageLastModified.after(imageLastModified)) {
                    ck += pageLastModified.getTimeInMillis() / 1000;
                } else if (imageLastModified != null) {
                    ck += imageLastModified.getTimeInMillis() / 1000;
                } else if (pageLastModified != null) {
                    ck += pageLastModified.getTimeInMillis() / 1000;
                }
            }

            return page.getPath() + ".thumb." + width + "." + height + ".png?ck=" + ck;
        }


        @Override
        public String getDescription() {
            return currentPage.getDescription();
        }

        @Override
        public String getSiteName() {
            Page page = findRootPage();

            String pageTitle = page.getPageTitle();
            if (StringUtils.isNotBlank(pageTitle)) {
                return pageTitle;
            }

            Resource content = page.getContentResource();
            if (content == null) {
                return null;
            }
            String title = content.getValueMap().get(JcrConstants.JCR_TITLE, String.class);
            if (StringUtils.isBlank(title)) {
                return null;
            }
            return title;
        }

        private Page findRootPage() {
            Page page = currentPage;
            while (true) {
                Page parent = page.getParent();
                if (parent == null) {
                    return page;
                } else {
                    page = parent;
                }
            }
        }
    }

    private class ProductMetadataProvider extends WebsiteMetadataProvider implements ProductMetadata {
        private Product product;
        private PriceInfo priceInfo;

        public ProductMetadataProvider(Product product) {
            this.product = product;
        }

        @Override
        public String getTitle() {
            String title = product.getTitle();
            if (StringUtils.isBlank(title)) {
                title = super.getTitle();
            }
            return title;
        }

        @Override
        public Type getType() {
            return Type.product;
        }

        @Override
        public String getImage() {
            final ImageResource imageResource = product.getImage();
            if (imageResource == null)
                return super.getImage();

            String image = imageResource.getFileReference();
            if (StringUtils.isBlank(image)) {
                image = super.getImage();
            } else {
                image = externalizer.publishLink(resourceResolver, image);
            }
            return image;
        }

        @Override
        public String getDescription() {
            String description = product.getDescription();
            if (StringUtils.isBlank(description)) {
                description = super.getDescription();
            }
            return description;
        }

        @Override
        public String getProductPriceAmount() {
            String amount = null;
            try {
                initPriceInfo();
                if (priceInfo != null) {
                    amount = String.valueOf(priceInfo.getAmount());
                }
            } catch (CommerceException x) {
                LOGGER.error("Error getting product price amount", x);
            }
            return amount;
        }

        @Override
        public String getProductPriceCurrency() {
            String currency = null;
            try {
                initPriceInfo();
                if (priceInfo != null) {
                    currency = priceInfo.getCurrency().getCurrencyCode();
                }
            } catch (CommerceException x) {
                LOGGER.error("Error getting product price currency", x);
            }
            return currency;
        }

        private void initPriceInfo() throws CommerceException {
            Resource productResource = resourceResolver.getResource(product.getPath());
            if (productResource != null) {
                CommerceService commerceService = productResource.adaptTo(CommerceService.class);
                if (commerceService != null) {
                    CommerceSession commerceSession = commerceService.login(request, response);
                    List<PriceInfo> priceInfoList = commerceSession.getProductPriceInfo(product, new PriceFilter("UNIT"));
                    if (!priceInfoList.isEmpty()) {
                        priceInfo = priceInfoList.get(0);
                    }
                }
            }
        }
    }

    private class ExperienceFragmentMetadataProvider {
        private ExperienceFragmentSocialVariation variation;

        public ExperienceFragmentMetadataProvider(ExperienceFragmentSocialVariation variation) {
            this.variation = variation;
        }

        public String getDescription(String defaultDescription) {
            if (variation == null)
                return defaultDescription;

            String description = variation.getText();
            if (StringUtils.isNotBlank(description)) {
                return description;
            }

            return defaultDescription;
        }

        public String getImage(String defaultImage) {
            if (variation == null)
                return defaultImage;

            String image = variation.getImagePath();
            if (StringUtils.isNotBlank(image)) {
                image = externalizer.publishLink(resourceResolver, image);
                return image;
            }

            return defaultImage;
        }
    }

    private class ExperienceFragmentWebsiteMetadataProvider extends WebsiteMetadataProvider {
        private final ExperienceFragmentMetadataProvider xfMetadata;

        public ExperienceFragmentWebsiteMetadataProvider(ExperienceFragmentSocialVariation variation) {
            xfMetadata = new ExperienceFragmentMetadataProvider(variation);
        }

        @Override
        public String getDescription() {
            return xfMetadata.getDescription(super.getDescription());
        }

        @Override
        public String getImage() {
            return xfMetadata.getImage(super.getImage());
        }
    }

    private class ExperienceFragmentProductMetadataProvider extends ProductMetadataProvider {
        private final ExperienceFragmentMetadataProvider xfMetadata;

        public ExperienceFragmentProductMetadataProvider(Product product, ExperienceFragmentSocialVariation variation) {
            super(product);
            xfMetadata = new ExperienceFragmentMetadataProvider(variation);
        }

        @Override
        public String getDescription() {
            return xfMetadata.getDescription(super.getDescription());
        }

        @Override
        public String getImage() {
            return xfMetadata.getImage(super.getImage());
        }
    }
}
