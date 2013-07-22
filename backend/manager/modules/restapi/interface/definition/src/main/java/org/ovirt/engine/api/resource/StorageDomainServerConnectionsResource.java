package org.ovirt.engine.api.resource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.jboss.resteasy.annotations.providers.jaxb.Formatted;
import org.ovirt.engine.api.model.StorageConnections;

@Produces({ ApiMediaType.APPLICATION_XML, ApiMediaType.APPLICATION_JSON, ApiMediaType.APPLICATION_X_YAML })
public interface StorageDomainServerConnectionsResource {
    @GET
    @Formatted
    public StorageConnections list();

    /**
     * Sub-resource locator method, returns individual StorageDomainServerConnectionResource on which the remainder of the URI is
     * dispatched.
     * @param id
     *            the StorageDomain ID
     * @return matching subresource if found
     */
    @Path("{id}")
    public StorageDomainServerConnectionResource getStorageConnectionSubResource(@PathParam("id") String id);
}
