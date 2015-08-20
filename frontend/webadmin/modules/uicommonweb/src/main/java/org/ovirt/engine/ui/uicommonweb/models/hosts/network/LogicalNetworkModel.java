package org.ovirt.engine.ui.uicommonweb.models.hosts.network;

import java.util.List;

import org.ovirt.engine.core.common.businessentities.network.Network;
import org.ovirt.engine.core.common.businessentities.network.NetworkBootProtocol;
import org.ovirt.engine.core.common.businessentities.network.NetworkStatus;
import org.ovirt.engine.core.common.businessentities.network.VdsNetworkInterface;
import org.ovirt.engine.core.common.businessentities.network.VdsNetworkInterface.NetworkImplementationDetails;
import org.ovirt.engine.core.common.businessentities.network.Vlan;
import org.ovirt.engine.ui.uicommonweb.models.hosts.DcNetworkParams;
import org.ovirt.engine.ui.uicommonweb.models.hosts.HostSetupNetworksModel;
import org.ovirt.engine.ui.uicommonweb.models.hosts.NetworkParameters;

/**
 * A Model for Logical Networks
 */
public class LogicalNetworkModel extends NetworkItemModel<NetworkStatus> {

    private boolean selected;
    private final boolean management;
    private boolean attachedViaLabel;
    private String errorMessage;
    private NetworkInterfaceModel attachedToNic;
    private NetworkInterfaceModel vlanNicModel;
    private Network network;

    public LogicalNetworkModel(Network network, HostSetupNetworksModel setupModel) {
        super(setupModel);
        setNetwork(network);
        management = network.getCluster() != null && network.getCluster().isManagement();
    }

    /**
     * attach a network to a target nic. If the network has VLAN id, it returns the newly created vlan bridge
     *
     * @param targetNic
     * @return
     */
    public VdsNetworkInterface attach(NetworkInterfaceModel targetNic, boolean createBridge) {
        attachedToNic = targetNic;
        List<LogicalNetworkModel> networksOnTarget = targetNic.getItems();
        networksOnTarget.add(this);

        if (!hasVlan()) {
            restoreNetworkParameters(targetNic.getIface());
        }

        if (isManagement()) {
            // mark the nic as a management nic
            targetNic.getIface().setType(2);
        }
        if (!createBridge) {
            return null;
        }
        VdsNetworkInterface targetNicEntity = targetNic.getIface();

        if (hasVlan()) {
            // create vlan bridge (eth0.1)
            VdsNetworkInterface bridge = new Vlan();
            bridge.setName(targetNic.getName() + "." + getVlanId()); //$NON-NLS-1$
            bridge.setNetworkName(getName());
            bridge.setBaseInterface(targetNic.getName());
            bridge.setVlanId(getVlanId());
            bridge.setMtu(getNetwork().getMtu());
            bridge.setVdsId(targetNicEntity.getVdsId());
            bridge.setVdsName(targetNicEntity.getVdsName());
            bridge.setBridged(getNetwork().isVmNetwork());
            restoreNetworkParameters(bridge);
            return bridge;
        } else {
            targetNicEntity.setNetworkName(getName());
            targetNicEntity.setMtu(getNetwork().getMtu());
            targetNicEntity.setBridged(getNetwork().isVmNetwork());
            return null;
        }
    }

    private void restoreNetworkParameters(VdsNetworkInterface nic) {
        NetworkParameters netParams = getSetupModel().getNetworkToLastDetachParams().get(getName());
        if (netParams != null) {
            nic.setBootProtocol(netParams.getBootProtocol());
            nic.setAddress(netParams.getAddress());
            nic.setSubnet(netParams.getSubnet());
            nic.setGateway(netParams.getGateway());
            //commenting out as we decided, has to be fixed in following separate UI patch
//            nic.setQosOverridden(netParams.getQosOverridden());
            nic.setQos(netParams.getQos());
            nic.setCustomProperties(netParams.getCustomProperties());
        } else if (nic.getBootProtocol() == null) {
            nic.setBootProtocol(isManagement() ? NetworkBootProtocol.DHCP : NetworkBootProtocol.NONE);
        }
    }

    public void detach() {
        boolean syncNetworkValues = false;
        if (!isInSync() && isManaged()) {
            getSetupModel().getNetworksToSync().add(getName());
            syncNetworkValues = true;
        }

        assert attachedToNic != null;
        NetworkInterfaceModel attachingNic = attachedToNic;
        // this needs to be null before the NIC items are changed, because they trigger an event
        attachedToNic = null;
        attachedViaLabel = false;
        List<LogicalNetworkModel> nicNetworks = attachingNic.getItems();
        nicNetworks.remove(this);
        // clear network name
        VdsNetworkInterface nicEntity = attachingNic.getIface();

        NetworkParameters netParams = new NetworkParameters();
        VdsNetworkInterface detachedDevice = hasVlan() ? vlanNicModel.getIface() : nicEntity;
        netParams.setBootProtocol(detachedDevice.getBootProtocol());
        netParams.setAddress(detachedDevice.getAddress());
        netParams.setSubnet(detachedDevice.getSubnet());
        netParams.setGateway(detachedDevice.getGateway());
        //commenting out as we decided, has to be fixed in following separate UI patch
//        netParams.setQosOverridden(detachedDevice.isQosOverridden());
        netParams.setQos(detachedDevice.getQos());
        netParams.setCustomProperties(detachedDevice.getCustomProperties());
        getSetupModel().getNetworkToLastDetachParams().put(getName(), netParams);

        if (!hasVlan()) {
            nicEntity.setNetworkName(null);
            nicEntity.setBootProtocol(null);
            nicEntity.setAddress(null);
            nicEntity.setSubnet(null);
            nicEntity.setGateway(null);
            //commenting out as we decided, has to be fixed in following separate UI patch
            //nicEntity.setQosOverridden(false);
            nicEntity.setQos(null);
            nicEntity.setCustomProperties(null);
            nicEntity.setNetworkImplementationDetails(null);
        }
        setVlanNicModel(null);
        // is this a management nic?
        if (nicEntity.getIsManagement()) {
            nicEntity.setType(0);
        }

        if (syncNetworkValues) {
            syncNetworkValues();
        }

    }

    private void syncNetworkValues() {
        DcNetworkParams dcNetParams = getSetupModel().getNetDcParams(getName());

        if (dcNetParams != null) {
            getNetwork().setVlanId(dcNetParams.getVlanId());
            getNetwork().setMtu(dcNetParams.getMtu());
            getNetwork().setVmNetwork(dcNetParams.isVmNetwork());
        }

    }

    public NetworkInterfaceModel getAttachedToNic() {
        return attachedToNic;
    }

    public NetworkInterfaceModel getVlanNicModel() {
        return vlanNicModel;
    }

    public Network getNetwork() {
        return network;
    }

    public void setNetwork(Network network) {
        this.network = network;
    }

    @Override
    public String getName() {
        return getNetwork().getName();
    }

    @Override
    public NetworkStatus getStatus() {
        return (getNetwork().getCluster() == null ? null : getNetwork().getCluster().getStatus());
    }

    public int getVlanId() {
        Integer vlanId = getNetwork().getVlanId();
        return vlanId == null ? -1 : vlanId;
    }

    public boolean hasVlan() {
        return getVlanId() >= 0;
    }

    public boolean isAttached() {
        return attachedToNic != null;
    }

    public boolean isAttachedViaLabel() {
        return attachedViaLabel;
    }

    public void attachViaLabel() {
        attachedViaLabel = true;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public boolean isManagement() {
        return management;
    }

    public Boolean isSelected() {
        return selected;
    }

    public void setVlanNicModel(NetworkInterfaceModel vlanNicmodel) {
        this.vlanNicModel = vlanNicmodel;
    }

    public void setSelected(Boolean selected) {
        this.selected = selected;
    }

    public boolean isInSync() {
        NetworkImplementationDetails details = getNetworkImplementationDetails();
        return details != null ? details.isInSync() : true;
    }

    public boolean isManaged() {
        NetworkImplementationDetails details = getNetworkImplementationDetails();
        return details != null ? details.isManaged() : true;
    }

    public NetworkImplementationDetails getNetworkImplementationDetails() {
        if (!isAttached()) {
            return null;
        }

        VdsNetworkInterface nic = hasVlan() ? getVlanNicModel().getIface() : getAttachedToNic().getIface();
        return nic.getNetworkImplementationDetails();
    }

    @Override
    public String getType() {
        return HostSetupNetworksModel.NETWORK;
    }
}
