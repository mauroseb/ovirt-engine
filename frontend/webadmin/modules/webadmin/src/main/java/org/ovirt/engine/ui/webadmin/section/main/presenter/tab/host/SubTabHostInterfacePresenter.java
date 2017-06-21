package org.ovirt.engine.ui.webadmin.section.main.presenter.tab.host;

import org.ovirt.engine.core.common.businessentities.VDS;
import org.ovirt.engine.ui.common.presenter.AbstractSubTabPresenter;
import org.ovirt.engine.ui.common.presenter.ExpandAllButtonPresenterWidget;
import org.ovirt.engine.ui.common.uicommon.model.SearchableDetailModelProvider;
import org.ovirt.engine.ui.common.widget.HasExpandAll;
import org.ovirt.engine.ui.uicommonweb.models.hosts.HostInterfaceLineModel;
import org.ovirt.engine.ui.uicommonweb.models.hosts.HostInterfaceListModel;
import org.ovirt.engine.ui.uicommonweb.models.hosts.HostListModel;
import org.ovirt.engine.ui.uicommonweb.place.WebAdminApplicationPlaces;
import org.ovirt.engine.ui.webadmin.section.main.presenter.tab.DetailTabDataIndex;
import org.ovirt.engine.ui.webadmin.section.main.view.tab.host.HostInterfaceActionPanelPresenterWidget;

import com.google.gwt.event.shared.EventBus;
import com.google.inject.Inject;
import com.gwtplatform.mvp.client.TabData;
import com.gwtplatform.mvp.client.annotations.NameToken;
import com.gwtplatform.mvp.client.annotations.ProxyCodeSplit;
import com.gwtplatform.mvp.client.annotations.TabInfo;
import com.gwtplatform.mvp.client.proxy.PlaceManager;
import com.gwtplatform.mvp.client.proxy.TabContentProxyPlace;

public class SubTabHostInterfacePresenter
    extends AbstractSubTabHostPresenter<HostInterfaceListModel, SubTabHostInterfacePresenter.ViewDef,
        SubTabHostInterfacePresenter.ProxyDef> implements HasExpandAll {

    @ProxyCodeSplit
    @NameToken(WebAdminApplicationPlaces.hostInterfaceSubTabPlace)
    public interface ProxyDef extends TabContentProxyPlace<SubTabHostInterfacePresenter> {
    }

    public interface ViewDef extends AbstractSubTabPresenter.ViewDef<VDS> {
        void expandAll();
        void collapseAll();
    }

    @TabInfo(container = HostSubTabPanelPresenter.class)
    static TabData getTabData() {
        return DetailTabDataIndex.HOSTS_IFACE;
    }

    @Inject
    public SubTabHostInterfacePresenter(EventBus eventBus, ViewDef view, ProxyDef proxy,
            PlaceManager placeManager, HostMainTabSelectedItems selectedItems,
            HostInterfaceActionPanelPresenterWidget actionPanel,
            ExpandAllButtonPresenterWidget expandAll,
            SearchableDetailModelProvider<HostInterfaceLineModel,
            HostListModel<Void>, HostInterfaceListModel> modelProvider) {
        super(eventBus, view, proxy, placeManager, modelProvider, selectedItems, actionPanel,
                HostSubTabPanelPresenter.TYPE_SetTabContent);
        expandAll.setTarget(this);
        actionPanel.setSearchPanel(expandAll);
    }

    @Override
    public void expandAll() {
        getView().expandAll();
    }

    @Override
    public void collapseAll() {
        getView().collapseAll();
    }

}
