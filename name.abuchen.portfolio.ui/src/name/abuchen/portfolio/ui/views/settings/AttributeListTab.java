package name.abuchen.portfolio.ui.views.settings;

import java.util.UUID;

import javax.inject.Inject;

import name.abuchen.portfolio.model.AttributeType;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.ClientSettings;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.util.Column;
import name.abuchen.portfolio.ui.util.ColumnEditingSupport;
import name.abuchen.portfolio.ui.util.ColumnEditingSupport.ModificationListener;
import name.abuchen.portfolio.ui.util.ContextMenu;
import name.abuchen.portfolio.ui.util.LabelOnly;
import name.abuchen.portfolio.ui.util.ShowHideColumnHelper;
import name.abuchen.portfolio.ui.util.StringEditingSupport;
import name.abuchen.portfolio.ui.util.ViewerHelper;
import name.abuchen.portfolio.ui.views.settings.SettingsView.Tab;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.preference.PreferenceStore;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

public class AttributeListTab implements Tab, ModificationListener
{
    @Inject
    private Client client;

    @Inject
    private PreferenceStore preferences;

    private TableViewer tableViewer;
    private ContextMenu contextMenuAdd;

    @Override
    public CTabItem createTab(CTabFolder folder)
    {
        Composite container = new Composite(folder, SWT.NONE);
        TableColumnLayout layout = new TableColumnLayout();
        container.setLayout(layout);

        tableViewer = new TableViewer(container, SWT.FULL_SELECTION | SWT.MULTI);

        ColumnEditingSupport.prepare(tableViewer);

        ShowHideColumnHelper support = new ShowHideColumnHelper(AttributeListTab.class.getSimpleName(), preferences,
                        tableViewer, layout);

        addColumns(support);

        support.createColumns();

        tableViewer.getTable().setHeaderVisible(true);
        tableViewer.getTable().setLinesVisible(true);
        tableViewer.setContentProvider(new ArrayContentProvider());

        ViewerHelper.pack(tableViewer);

        tableViewer.setInput(client.getSettings().getAttributeTypes().toArray());
        tableViewer.refresh();

        new ContextMenu(tableViewer.getTable(), m -> fillContextMenu(m)).hook();

        CTabItem item = new CTabItem(folder, SWT.NONE);
        item.setText(Messages.AttributeTypeTitle);
        item.setControl(container);
        return item;
    }

    private void addColumns(ShowHideColumnHelper support)
    {
        Column column = new Column(Messages.ColumnName, SWT.None, 250);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                return ((AttributeType) element).getName();
            }

            @Override
            public Image getImage(Object element)
            {
                return PortfolioPlugin.image(PortfolioPlugin.IMG_TEXT);
            }
        });
        new StringEditingSupport(AttributeType.class, "name").addListener(this).attachTo(column); //$NON-NLS-1$
        support.addColumn(column);

        column = new Column(Messages.ColumnColumnLabel, SWT.None, 150);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                return ((AttributeType) element).getColumnLabel();
            }
        });
        new StringEditingSupport(AttributeType.class, "columnLabel").addListener(this).attachTo(column); //$NON-NLS-1$
        support.addColumn(column);

        column = new Column(Messages.ColumnFieldType, SWT.None, 150);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                return AttributeFieldType.of((AttributeType) element).toString();
            }
        });
        support.addColumn(column);
    }

    private void fillContextMenu(IMenuManager manager)
    {
        IStructuredSelection selection = (IStructuredSelection) tableViewer.getSelection();
        AttributeType attributeType = (AttributeType) selection.getFirstElement();

        if (selection.size() == 1)
            addMoveUpAndDownActions(manager, attributeType);

        manager.add(new Separator());
        addDeleteActions(manager, selection);
    }

    private void addMoveUpAndDownActions(IMenuManager manager, AttributeType attributeType)
    {
        int index = tableViewer.getTable().getSelectionIndex();

        if (index < 0)
            return;

        if (index > 0)
        {
            manager.add(new Action(Messages.BookmarksListView_MoveUp)
            {
                @Override
                public void run()
                {
                    ClientSettings settings = client.getSettings();
                    settings.removeAttributeType(attributeType);
                    settings.addAttributeType(index - 1, attributeType);
                    tableViewer.setInput(client.getSettings().getAttributeTypes().toArray());
                    client.markDirty();
                }
            });
        }

        if (index < tableViewer.getTable().getItemCount() - 1)
        {
            manager.add(new Action(Messages.BookmarksListView_MoveDown)
            {
                @Override
                public void run()
                {
                    ClientSettings settings = client.getSettings();
                    settings.removeAttributeType(attributeType);
                    settings.addAttributeType(index + 1, attributeType);
                    tableViewer.setInput(client.getSettings().getAttributeTypes().toArray());
                    client.markDirty();
                }
            });
        }
    }

    private void addDeleteActions(IMenuManager manager, IStructuredSelection selection)
    {
        manager.add(new Separator());
        manager.add(new Action(Messages.BookmarksListView_delete)
        {
            @Override
            public void run()
            {
                ClientSettings settings = client.getSettings();
                for (Object element : selection.toArray())
                {
                    AttributeType attribute = (AttributeType) element;
                    // remove any existing attribute values from securities
                    for (Security security : client.getSecurities())
                        security.getAttributes().remove(attribute);
                    settings.removeAttributeType(attribute);
                }

                client.markDirty();
                tableViewer.setInput(settings.getAttributeTypes().toArray());
            }
        });
    }

    @Override
    public void onModified(Object element, Object newValue, Object oldValue)
    {
        client.markDirty();
    }

    @Override
    public void showAddMenu(Shell shell)
    {
        if (contextMenuAdd == null)
            contextMenuAdd = new ContextMenu(tableViewer.getControl(), m -> fillAddMenu(m));

        contextMenuAdd.getMenu().setVisible(true);
    }

    private void fillAddMenu(IMenuManager manager)
    {
        manager.add(new LabelOnly(Messages.LabelNewFieldByType));

        for (AttributeFieldType fieldType : AttributeFieldType.values())
        {
            manager.add(new Action(fieldType.toString())
            {
                @Override
                public void run()
                {
                    AttributeType attributeType = new AttributeType(UUID.randomUUID().toString());
                    attributeType.setName(Messages.ColumnName);
                    attributeType.setColumnLabel(Messages.ColumnColumnLabel);
                    attributeType.setConverter(fieldType.getConverterClass());
                    attributeType.setType(fieldType.getFieldClass());
                    // only security supported currently
                    attributeType.setTarget(Security.class);

                    client.getSettings().addAttributeType(attributeType);
                    tableViewer.setInput(client.getSettings().getAttributeTypes().toArray());
                    client.markDirty();

                    tableViewer.editElement(attributeType, 0);
                }
            });
        }
    }
}
