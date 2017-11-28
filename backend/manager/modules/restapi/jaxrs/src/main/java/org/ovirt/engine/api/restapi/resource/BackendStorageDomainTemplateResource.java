package org.ovirt.engine.api.restapi.resource;

import javax.ws.rs.core.Response;

import org.ovirt.engine.api.model.Action;
import org.ovirt.engine.api.model.Template;
import org.ovirt.engine.api.model.Templates;
import org.ovirt.engine.api.resource.ActionResource;
import org.ovirt.engine.api.resource.StorageDomainContentDisksResource;
import org.ovirt.engine.api.resource.StorageDomainTemplateResource;
import org.ovirt.engine.api.restapi.types.ExternalRegistrationConfigurationMapper;
import org.ovirt.engine.api.restapi.types.ExternalVnicProfileMappingMapper;
import org.ovirt.engine.core.common.action.ActionType;
import org.ovirt.engine.core.common.action.ImportVmTemplateFromConfParameters;
import org.ovirt.engine.core.common.action.ImportVmTemplateParameters;
import org.ovirt.engine.core.common.action.VmTemplateImportExportParameters;
import org.ovirt.engine.core.common.businessentities.VmTemplate;
import org.ovirt.engine.core.common.businessentities.storage.Disk;
import org.ovirt.engine.core.common.businessentities.storage.DiskImage;
import org.ovirt.engine.core.common.queries.GetVmTemplateParameters;
import org.ovirt.engine.core.common.queries.QueryType;
import org.ovirt.engine.core.compat.Guid;

public class BackendStorageDomainTemplateResource
    extends AbstractBackendStorageDomainContentResource<Templates, Template, VmTemplate>
    implements StorageDomainTemplateResource {

    VmTemplate template;

    public BackendStorageDomainTemplateResource(BackendStorageDomainTemplatesResource parent, String templateId) {
        super(templateId, parent, Template.class, VmTemplate.class);
    }

    @Override
    public Template get() {
        switch (parent.getStorageDomainType()) {
        case Data:
        case Master:
            return getFromDataDomain();
        case ImportExport:
            return getFromExportDomain();
        default:
            return null;
        }
    }

    private Template getFromDataDomain() {
        return performGet(QueryType.GetVmTemplate, new GetVmTemplateParameters(guid));
    }

    private Template getFromExportDomain() {
        org.ovirt.engine.core.common.businessentities.VmTemplate entity = getEntity();
        return addLinks(populate(map(entity, null), entity), null, new String[0]);
    }

    @Override
    public Response register(Action action) {
        BackendVnicProfileHelper.validateVnicMappings(this, action);
        ImportVmTemplateFromConfParameters params = new ImportVmTemplateFromConfParameters();
        ExternalRegistrationConfigurationMapper.mapFromModel(action.getRegistrationConfiguration(), params);
        params.setContainerId(guid);
        params.setStorageDomainId(parent.getStorageDomainId());
        if (action.isSetCluster()) {
            params.setClusterId(getClusterId(action));
        }
        params.setImagesExistOnTargetStorageDomain(true);
        params.setExternalVnicProfileMappings(ExternalVnicProfileMappingMapper.mapFromModel(action.getVnicProfileMappings()));

        if (action.isSetClone()) {
            params.setImportAsNewEntity(action.isClone());
            if (action.isSetVm() && action.getTemplate().isSetName()) {
                params.getVmTemplate().setName(action.getTemplate().getName());
            }
        }
        if (action.isSetAllowPartialImport()) {
            params.setAllowPartialImport(action.isAllowPartialImport());
        }

        return doAction(ActionType.ImportVmTemplateFromConfiguration, params, action);
    }

    @Override
    public Response doImport(Action action) {
        validateParameters(action, "storageDomain.id|name");
        Guid destStorageDomainId = getDestStorageDomainId(action);
        Guid clusterId = null;
        if (action.isSetCluster()) {
            clusterId = getClusterId(action);
        }
        ImportVmTemplateParameters params = new ImportVmTemplateParameters(parent.getDataCenterId(destStorageDomainId),
                                                                           parent.getStorageDomainId(),
                                                                           destStorageDomainId,
                                                                           clusterId,
                                                                           getEntity());
        params.setImageToDestinationDomainMap(getDiskToDestinationMap(action));
        params.setForceOverride(action.isSetExclusive() ? action.isExclusive() : false);

        if (action.isSetClone()){
            params.setImportAsNewEntity(action.isClone());
            if(action.isSetTemplate() && action.getTemplate().isSetName()) {
                params.getVmTemplate().setName(action.getTemplate().getName());
            }
        }

        return doAction(ActionType.ImportVmTemplate, params, action);
    }

    @Override
    public ActionResource getActionResource(String action, String ids) {
        return inject(new BackendActionResource(action, ids));
    }

    @Override
    protected Template addParents(Template template) {
        template.setStorageDomain(parent.getStorageDomainModel());
        return template;
    }

    protected VmTemplate getEntity() {
        if (template != null) {
            return template;
        }
        for (VmTemplate entity : parent.getEntitiesFromExportDomain()) {
            if (guid.equals(entity.getId())) {
                template = entity;
                return entity;
            }
        }
        return entityNotFound();
    }

    @Override
    public java.util.Map<Guid, Disk> getDiskMap() {
        java.util.Map<Guid, Disk> diskMap = new java.util.HashMap<>();
        for (java.util.Map.Entry<Guid, DiskImage> entry : getEntity().getDiskTemplateMap().entrySet()) {
            diskMap.put(entry.getKey(), entry.getValue());
        }
        return diskMap;
    }

    @Override
    public StorageDomainContentDisksResource getDisksResource() {
        return inject(new BackendExportDomainDisksResource(this));
    }

    @Override
    public Response remove() {
        get();
        return performAction(ActionType.RemoveVmTemplateFromImportExport,
                new VmTemplateImportExportParameters(guid,
                        parent.storageDomainId,
                        getDataCenterId(parent.storageDomainId)));
    }
}
