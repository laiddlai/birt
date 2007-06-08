/*******************************************************************************
 * Copyright (c) 2005 Actuate Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *  Actuate Corporation  - initial API and implementation
 *******************************************************************************/

package org.eclipse.birt.report.designer.ui.cubebuilder.page;

import java.util.Iterator;

import org.eclipse.birt.report.designer.core.commands.DeleteCommand;
import org.eclipse.birt.report.designer.core.model.SessionHandleAdapter;
import org.eclipse.birt.report.designer.internal.ui.util.ExceptionHandler;
import org.eclipse.birt.report.designer.internal.ui.util.UIUtil;
import org.eclipse.birt.report.designer.internal.ui.views.RenameInputDialog;
import org.eclipse.birt.report.designer.internal.ui.views.outline.ListenerElementVisitor;
import org.eclipse.birt.report.designer.ui.cubebuilder.dialog.DateGroupDialog;
import org.eclipse.birt.report.designer.ui.cubebuilder.dialog.DateLevelDialog;
import org.eclipse.birt.report.designer.ui.cubebuilder.dialog.LevelPropertyDialog;
import org.eclipse.birt.report.designer.ui.cubebuilder.dialog.MeasureDialog;
import org.eclipse.birt.report.designer.ui.cubebuilder.nls.Messages;
import org.eclipse.birt.report.designer.ui.cubebuilder.provider.CubeContentProvider;
import org.eclipse.birt.report.designer.ui.cubebuilder.provider.CubeLabelProvider;
import org.eclipse.birt.report.designer.ui.cubebuilder.provider.DataContentProvider;
import org.eclipse.birt.report.designer.ui.cubebuilder.util.OlapUtil;
import org.eclipse.birt.report.designer.ui.cubebuilder.util.UIHelper;
import org.eclipse.birt.report.designer.ui.cubebuilder.util.VirtualField;
import org.eclipse.birt.report.designer.ui.newelement.DesignElementFactory;
import org.eclipse.birt.report.designer.util.DEUtil;
import org.eclipse.birt.report.model.api.DataSetHandle;
import org.eclipse.birt.report.model.api.DesignElementHandle;
import org.eclipse.birt.report.model.api.LevelAttributeHandle;
import org.eclipse.birt.report.model.api.PropertyHandle;
import org.eclipse.birt.report.model.api.ResultSetColumnHandle;
import org.eclipse.birt.report.model.api.activity.NotificationEvent;
import org.eclipse.birt.report.model.api.activity.SemanticException;
import org.eclipse.birt.report.model.api.command.ContentEvent;
import org.eclipse.birt.report.model.api.command.NameException;
import org.eclipse.birt.report.model.api.core.IDesignElement;
import org.eclipse.birt.report.model.api.core.Listener;
import org.eclipse.birt.report.model.api.elements.DesignChoiceConstants;
import org.eclipse.birt.report.model.api.olap.CubeHandle;
import org.eclipse.birt.report.model.api.olap.DimensionHandle;
import org.eclipse.birt.report.model.api.olap.LevelHandle;
import org.eclipse.birt.report.model.api.olap.MeasureGroupHandle;
import org.eclipse.birt.report.model.api.olap.MeasureHandle;
import org.eclipse.birt.report.model.api.olap.TabularCubeHandle;
import org.eclipse.birt.report.model.api.olap.TabularDimensionHandle;
import org.eclipse.birt.report.model.api.olap.TabularHierarchyHandle;
import org.eclipse.birt.report.model.api.olap.TabularLevelHandle;
import org.eclipse.birt.report.model.api.olap.TabularMeasureHandle;
import org.eclipse.birt.report.model.elements.interfaces.ICubeModel;
import org.eclipse.birt.report.model.elements.interfaces.IDimensionModel;
import org.eclipse.birt.report.model.elements.interfaces.IHierarchyModel;
import org.eclipse.birt.report.model.elements.interfaces.IMeasureGroupModel;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSource;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TreeItem;

public class CubeGroupContent extends Composite implements Listener
{

	private TreeItem[] dragSourceItems = new TreeItem[1];

	class CustomDragListener implements DragSourceListener
	{

		private TreeViewer viewer;

		CustomDragListener( TreeViewer viewer )
		{
			this.viewer = viewer;
		}

		public void dragFinished( DragSourceEvent event )
		{

		};

		public void dragSetData( DragSourceEvent event )
		{
			event.data = dragSourceItems[0].getText( );
		}

		public void dragStart( DragSourceEvent event )
		{
			TreeItem[] selection = viewer.getTree( ).getSelection( );

			if ( selection.length > 0 )
			{
				if ( viewer == dataFieldsViewer )
				{
					dragSourceItems[0] = selection[0];
				}
				else if ( viewer == groupViewer
						&& selection[0].getData( ) != null
						&& selection[0].getData( ) instanceof LevelHandle )
				{
					dragSourceItems[0] = selection[0];
				}
				else
					event.doit = false;
			}
			else
			{
				event.doit = false;
			}

		}
	}

	private TreeViewer dataFieldsViewer;

	public CubeGroupContent( Composite parent, int style )
	{
		super( parent, style );
		GridLayout layout = new GridLayout( 4, false );
		layout.marginTop = 0;
		this.setLayout( layout );
		createContent( );
	}

	private CubeBuilder builder;

	public CubeGroupContent( CubeBuilder builder, Composite parent, int style )
	{
		super( parent, style );
		this.builder = builder;
		GridLayout layout = new GridLayout( 4, false );
		layout.marginTop = 0;
		this.setLayout( layout );
		createContent( );
	}

	public void dispose( )
	{
		if ( visitor != null )
		{
			if ( input != null )
				visitor.removeListener( input );
			visitor.dispose( );
			visitor = null;
		}
		super.dispose( );
	}

	private TabularCubeHandle input;
	private TreeViewer groupViewer;

	public void setInput( TabularCubeHandle cube )
	{
		if ( input != null )
			getListenerElementVisitor( ).removeListener( input );
		this.input = cube;
	}
	private DataSetHandle[] datasets = new DataSetHandle[1];

	public void setInput( TabularCubeHandle cube, DataSetHandle dataset )
	{
		this.input = cube;
		datasets[0] = dataset;
	}

	public void createContent( )
	{
		createDataField( );
		createMoveButtonsField( );
		createGroupField( );
		createOperationField( );
	}

	private void createOperationField( )
	{
		Composite operationField = new Composite( this, SWT.NONE );
		operationField.setLayout( new GridLayout( ) );
		operationField.setLayout( new GridLayout( ) );

		String[] btnTexts = new String[]{
				Messages.getString( "GroupsPage.Button.Add" ), //$NON-NLS-1$
				Messages.getString( "GroupsPage.Button.Edit" ), //$NON-NLS-1$
				Messages.getString( "GroupsPage.Button.Delete" ), //$NON-NLS-1$
		};
		addBtn = new Button( operationField, SWT.PUSH );
		addBtn.setText( btnTexts[0] );
		addBtn.addSelectionListener( new SelectionAdapter( ) {

			public void widgetSelected( SelectionEvent e )
			{
				handleAddEvent( );
			}

		} );

		editBtn = new Button( operationField, SWT.PUSH );
		editBtn.setText( btnTexts[1] );
		editBtn.addSelectionListener( new SelectionAdapter( ) {

			public void widgetSelected( SelectionEvent e )
			{
				handleEditEvent( );
			}

		} );

		delBtn = new Button( operationField, SWT.PUSH );
		delBtn.setText( btnTexts[2] );
		delBtn.addSelectionListener( new SelectionAdapter( ) {

			public void widgetSelected( SelectionEvent e )
			{
				handleDelEvent( );
			}

		} );

		int width = UIUtil.getMaxStringWidth( btnTexts, operationField );
		if ( width < 60 )
			width = 60;
		layoutButton( addBtn, width );
		layoutButton( editBtn, width );
		layoutButton( delBtn, width );
		addBtn.setEnabled( false );
		editBtn.setEnabled( false );
		delBtn.setEnabled( false );

		GridData data = (GridData) addBtn.getLayoutData( );
		data.grabExcessVerticalSpace = true;
		data.verticalAlignment = SWT.BOTTOM;

		data = (GridData) delBtn.getLayoutData( );
		data.grabExcessVerticalSpace = true;
		data.verticalAlignment = SWT.TOP;

	}

	private void layoutButton( Button button, int width )
	{
		GridData gd = new GridData( );
		gd.widthHint = width;
		button.setLayoutData( gd );
	}

	private void createGroupField( )
	{
		Composite groupField = new Composite( this, SWT.NONE );
		groupField.setLayoutData( new GridData( GridData.FILL_BOTH ) );
		groupField.setLayout( new GridLayout( ) );

		Label groupLabel = new Label( groupField, SWT.NONE );
		groupLabel.setText( Messages.getString( "GroupsPage.Label.Group" ) ); //$NON-NLS-1$

		groupViewer = new TreeViewer( groupField, SWT.SINGLE
				| SWT.H_SCROLL
				| SWT.V_SCROLL
				| SWT.BORDER );
		groupViewer.getTree( )
				.setLayoutData( new GridData( GridData.FILL_BOTH ) );
		( (GridData) groupViewer.getTree( ).getLayoutData( ) ).heightHint = 250;
		( (GridData) groupViewer.getTree( ).getLayoutData( ) ).widthHint = 250;
		groupViewer.setLabelProvider( new CubeLabelProvider( ) );
		groupViewer.setContentProvider( new CubeContentProvider( ) );
		groupViewer.addSelectionChangedListener( new ISelectionChangedListener( ) {

			public void selectionChanged( SelectionChangedEvent event )
			{
				updateButtons( );
			}

		} );
		groupViewer.setAutoExpandLevel( 4 );
		groupViewer.getTree( ).addKeyListener( new KeyAdapter( ) {

			public void keyPressed( KeyEvent e )
			{
				if ( e.keyCode == SWT.DEL )
				{
					try
					{
						handleDelEvent( );
					}
					catch ( Exception e1 )
					{
						ExceptionHandler.handle( e1 );
					}
				}
			}
		} );

		final DragSource fieldsSource = new DragSource( groupViewer.getTree( ),
				operations );
		fieldsSource.setTransfer( types );
		fieldsSource.addDragListener( new CustomDragListener( groupViewer ) );

		DropTarget target = new DropTarget( groupViewer.getTree( ), operations );
		target.setTransfer( types );
		target.addDropListener( new DropTargetAdapter( ) {

			public void dragOver( DropTargetEvent event )
			{
				event.feedback = DND.FEEDBACK_EXPAND | DND.FEEDBACK_SCROLL;
				if ( event.item != null )
				{
					TreeItem item = (TreeItem) event.item;
					Object element = item.getData( );
					if ( element instanceof CubeHandle
							|| element instanceof PropertyHandle )
					{
						event.detail = DND.DROP_NONE;
						return;
					}

					Object obj = (Object) dragSourceItems[0].getData( );
					ResultSetColumnHandle dataField = null;
					DataSetHandle dataset = null;
					if ( obj == null
							|| obj instanceof DataSetHandle
							|| ( obj instanceof VirtualField && ( (VirtualField) obj ).getType( )
									.equals( VirtualField.TYPE_OTHER_DATASETS ) ) )
					{
						event.detail = DND.DROP_NONE;
						return;
					}

					if ( obj instanceof ResultSetColumnHandle )
					{
						dataField = (ResultSetColumnHandle) obj;
						dataset = (DataSetHandle) dataField.getElementHandle( );

						if ( element instanceof LevelHandle )
						{
							DataSetHandle temp = ( (TabularHierarchyHandle) ( (LevelHandle) element ).getContainer( ) ).getDataSet( );
							if ( temp != null
									&& dataset != null
									&& dataset != temp )
							{
								event.detail = DND.DROP_NONE;
								return;
							}

							if ( dataField != null
									&& isDateType( dataField.getDataType( ) ) )
							{
								event.detail = DND.DROP_NONE;
								return;
							}

							DesignElementHandle hierarchy = ( (TabularLevelHandle) element ).getContainer( );
							DimensionHandle dimension = (DimensionHandle) hierarchy.getContainer( );

							if ( dimension.isTimeType( ) )
							{
								event.detail = DND.DROP_NONE;
								return;
							}

						}
						else if ( element instanceof DimensionHandle
								|| ( element instanceof VirtualField && ( (VirtualField) element ).getType( )
										.equals( VirtualField.TYPE_LEVEL ) ) )
						{
							DimensionHandle dimension = null;
							if ( element instanceof DimensionHandle )
								dimension = (DimensionHandle) element;
							else
								dimension = (DimensionHandle) ( (VirtualField) element ).getModel( );

							if ( dimension.isTimeType( ) )
							{
								event.detail = DND.DROP_NONE;
								return;
							}

							DataSetHandle temp = ( (TabularHierarchyHandle) dimension.getDefaultHierarchy( ) ).getDataSet( );
							if ( temp != null
									&& dataset != null
									&& dataset != temp )
							{
								event.detail = DND.DROP_NONE;
								return;
							}
							TabularHierarchyHandle hierarchy = ( (TabularHierarchyHandle) dimension.getDefaultHierarchy( ) );
							if ( hierarchy.getContentCount( IHierarchyModel.LEVELS_PROP ) > 0 )
							{
								if ( dataField != null
										&& isDateType( dataField.getDataType( ) ) )
								{
									event.detail = DND.DROP_NONE;
									return;
								}
							}

						}
						else if ( element instanceof MeasureGroupHandle
								|| ( element instanceof VirtualField && ( (VirtualField) element ).getType( )
										.equals( VirtualField.TYPE_MEASURE ) )
								|| element instanceof MeasureHandle
								|| ( element instanceof VirtualField && ( (VirtualField) element ).getType( )
										.equals( VirtualField.TYPE_MEASURE_GROUP ) ) )
						{
							DataSetHandle primary = ( input ).getDataSet( );
							if ( primary == null || primary != dataset )
							{
								event.detail = DND.DROP_NONE;
								return;
							}
						}
					}

					if ( obj instanceof LevelHandle )
					{
						if ( !( element instanceof LevelHandle )
								|| element == obj
								|| ( (LevelHandle) obj ).getContainer( ) != ( (LevelHandle) element ).getContainer( ) )
						{
							event.detail = DND.DROP_NONE;
							return;
						}
					}

					Point pt = Display.getCurrent( ).map( null,
							groupViewer.getTree( ),
							event.x,
							event.y );
					Rectangle bounds = item.getBounds( );
					if ( pt.y < bounds.y + bounds.height / 3 )
					{
						event.feedback |= DND.FEEDBACK_INSERT_BEFORE;
					}
					else if ( pt.y > bounds.y + 2 * bounds.height / 3 )
					{
						event.feedback |= DND.FEEDBACK_INSERT_AFTER;
					}
					else
					{
						event.feedback |= DND.FEEDBACK_SELECT;
					}
				}
				else
				{
					event.detail = DND.DROP_NONE;
				}

			}

			public void drop( DropTargetEvent event )
			{
				if ( event.data == null )
				{
					event.detail = DND.DROP_NONE;
					return;
				}

				Object obj = (Object) dragSourceItems[0].getData( );
				ResultSetColumnHandle dataField = null;
				DataSetHandle dataset = null;
				if ( obj == null || obj instanceof DataSetHandle )
				{
					event.detail = DND.DROP_NONE;
					return;
				}

				if ( obj instanceof ResultSetColumnHandle )
				{
					dataField = (ResultSetColumnHandle) obj;
					dataset = (DataSetHandle) dataField.getElementHandle( );

					if ( event.item == null || dataField == null )
					{
						event.detail = DND.DROP_NONE;
						return;
					}
					else
					{

						TreeItem item = (TreeItem) event.item;
						Point pt = Display.getCurrent( ).map( null,
								groupViewer.getTree( ),
								event.x,
								event.y );
						Rectangle bounds = item.getBounds( );

						Object element = item.getData( );
						try
						{
							if ( pt.y < bounds.y + bounds.height / 3 )
							{
								if ( element instanceof MeasureHandle )
								{
									TabularMeasureHandle measure = DesignElementFactory.getInstance( )
											.newTabularMeasure( dataField.getColumnName( ) );
									measure.setMeasureExpression( DEUtil.getExpression( dataField ) );
									measure.setDataType( dataField.getDataType( ) );
									( (MeasureHandle) element ).getContainer( )
											.add( IMeasureGroupModel.MEASURES_PROP,
													measure );
									MeasureDialog dialog = new MeasureDialog( false );
									dialog.setInput( input, measure );
									dialog.open( );
								}
								else if ( element instanceof CubeHandle
										|| element instanceof PropertyHandle )
								{
									event.detail = DND.DROP_NONE;
									return;
								}
								else if ( element instanceof LevelHandle )
								{
									DesignElementHandle hierarchy = ( (TabularLevelHandle) element ).getContainer( );
									DimensionHandle dimension = (DimensionHandle) hierarchy.getContainer( );

									if ( dimension.isTimeType( ) )
									{
										event.detail = DND.DROP_NONE;
										return;
									}

									int index = ( (LevelHandle) element ).getIndex( );
									TabularLevelHandle level = DesignElementFactory.getInstance( )
											.newTabularLevel( dimension,
													dataField.getColumnName( ) );
									level.setColumnName( dataField.getColumnName( ) );
									level.setDataType( dataField.getDataType( ) );
									( (LevelHandle) element ).getContainer( )
											.add( IHierarchyModel.LEVELS_PROP,
													level,
													index );
									LevelPropertyDialog dialog = new LevelPropertyDialog( true );
									dialog.setInput( level );
									dialog.open( );
								}
							}
							else
							{
								if ( element instanceof MeasureHandle )
								{
									TabularMeasureHandle measure = DesignElementFactory.getInstance( )
											.newTabularMeasure( dataField.getColumnName( ) );
									measure.setMeasureExpression( DEUtil.getExpression( dataField ) );
									measure.setDataType( dataField.getDataType( ) );
									( (MeasureHandle) element ).getContainer( )
											.add( IMeasureGroupModel.MEASURES_PROP,
													measure );
									MeasureDialog dialog = new MeasureDialog( false );
									dialog.setInput( input, measure );
									dialog.open( );
								}
								else if ( element instanceof MeasureGroupHandle
										|| ( element instanceof VirtualField && ( (VirtualField) element ).getType( )
												.equals( VirtualField.TYPE_MEASURE ) )
										|| ( element instanceof VirtualField && ( (VirtualField) element ).getType( )
												.equals( VirtualField.TYPE_MEASURE_GROUP ) ) )
								{
									MeasureGroupHandle measureGroup = null;
									if ( ( element instanceof VirtualField && ( (VirtualField) element ).getType( )
											.equals( VirtualField.TYPE_MEASURE_GROUP ) ) )
									{
										measureGroup = DesignElementFactory.getInstance( )
												.newTabularMeasureGroup( null ); //$NON-NLS-1$
										input.add( CubeHandle.MEASURE_GROUPS_PROP,
												measureGroup );
										if ( input.getContentCount( ICubeModel.MEASURE_GROUPS_PROP ) == 1 )
											input.setDefaultMeasureGroup( measureGroup );
									}
									else
									{
										if ( element instanceof MeasureGroupHandle )
											measureGroup = (MeasureGroupHandle) element;
										else
											measureGroup = (MeasureGroupHandle) ( (VirtualField) element ).getModel( );
									}
									TabularMeasureHandle measure = DesignElementFactory.getInstance( )
											.newTabularMeasure( dataField.getColumnName( ) );
									measure.setMeasureExpression( DEUtil.getExpression( dataField ) );
									measure.setDataType( dataField.getDataType( ) );
									measureGroup.add( IMeasureGroupModel.MEASURES_PROP,
											measure );
									MeasureDialog dialog = new MeasureDialog( false );
									dialog.setInput( input, measure );
									dialog.open( );
								}
								else if ( element instanceof CubeHandle
										|| element instanceof PropertyHandle )
								{
									event.detail = DND.DROP_NONE;
									return;
								}
								else if ( element instanceof LevelHandle )
								{
									if ( isDateType( dataField.getDataType( ) ) )
									{
										event.detail = DND.DROP_NONE;
										return;
									}

									TabularHierarchyHandle hierarchy = (TabularHierarchyHandle) ( (LevelHandle) element ).getContainer( );
									DimensionHandle dimension = (DimensionHandle) hierarchy.getContainer( );

									if ( dimension.isTimeType( ) )
									{
										event.detail = DND.DROP_NONE;
										return;
									}

									if ( hierarchy.getDataSet( ) == null
											&& dataset != null )
									{
										hierarchy.setDataSet( dataset );
									}

									int index = ( (LevelHandle) element ).getIndex( );
									TabularLevelHandle level = DesignElementFactory.getInstance( )
											.newTabularLevel( dimension,
													dataField.getColumnName( ) );
									level.setColumnName( dataField.getColumnName( ) );
									level.setDataType( dataField.getDataType( ) );
									( (LevelHandle) element ).getContainer( )
											.add( IHierarchyModel.LEVELS_PROP,
													level,
													index + 1 );
									LevelPropertyDialog dialog = new LevelPropertyDialog( true );
									dialog.setInput( level );
									dialog.open( );
								}
								else if ( element instanceof DimensionHandle
										|| ( element instanceof VirtualField && ( (VirtualField) element ).getType( )
												.equals( VirtualField.TYPE_LEVEL ) )
										|| ( element instanceof VirtualField && ( (VirtualField) element ).getType( )
												.equals( VirtualField.TYPE_DIMENSION ) ) )
								{
									DimensionHandle dimension = null;
									if ( element instanceof VirtualField
											&& ( (VirtualField) element ).getType( )
													.equals( VirtualField.TYPE_DIMENSION ) )
									{
										dimension = DesignElementFactory.getInstance( )
												.newTabularDimension( null ); //$NON-NLS-1$
										input.add( CubeHandle.DIMENSIONS_PROP,
												dimension );
									}
									else
									{
										if ( element instanceof DimensionHandle )
											dimension = (DimensionHandle) element;
										else
											dimension = (DimensionHandle) ( (VirtualField) element ).getModel( );
									}
									if ( dimension.isTimeType( ) )
									{
										event.detail = DND.DROP_NONE;
										return;
									}

									TabularHierarchyHandle hierarchy = (TabularHierarchyHandle) dimension.getDefaultHierarchy( );

									if ( hierarchy.getDataSet( ) == null
											&& dataset != null )
									{
										hierarchy.setDataSet( dataset );

									}

									if ( isDateType( dataField.getDataType( ) ) )
									{
										if ( hierarchy.getContentCount( IHierarchyModel.LEVELS_PROP ) > 0 )
										{
											event.detail = DND.DROP_NONE;
											return;
										}
										else
										{
											DateGroupDialog dialog = new DateGroupDialog( );
											dialog.setInput( hierarchy,
													dataField.getColumnName( ) );
											if ( dialog.open( ) == Window.OK )
											{
												dimension.setTimeType( true );
											}
											else
											{
												boolean hasExecuted = OlapUtil.enableDrop( dimension );
												if ( hasExecuted )
												{
													UIHelper.dropDimensionProperties( dimension );
													new DeleteCommand( dimension ).execute( );
													refresh( );
													return;
												}
											}
										}
									}
									else
									{
										TabularLevelHandle level = DesignElementFactory.getInstance( )
												.newTabularLevel( dimension,
														dataField.getColumnName( ) );
										level.setColumnName( dataField.getColumnName( ) );
										level.setDataType( dataField.getDataType( ) );
										hierarchy.add( IHierarchyModel.LEVELS_PROP,
												level );
										LevelPropertyDialog dialog = new LevelPropertyDialog( true );
										dialog.setInput( level );
										dialog.open( );
									}
									if ( dataset != input.getDataSet( ) )
									{
										builder.showSelectionPage( builder.getLinkGroupNode( ) );
									}
								}
							}
						}
						catch ( SemanticException e )
						{
							ExceptionHandler.handle( e );
						}
					}
				}

				if ( obj instanceof LevelHandle )
				{
					int oldIndex = ( (LevelHandle) obj ).getIndex( );
					if ( event.item == null )
					{
						event.detail = DND.DROP_NONE;
						return;
					}
					else
					{

						TreeItem item = (TreeItem) event.item;
						Point pt = Display.getCurrent( ).map( null,
								groupViewer.getTree( ),
								event.x,
								event.y );
						Rectangle bounds = item.getBounds( );

						LevelHandle element = (LevelHandle) item.getData( );
						int newIndex = element.getIndex( );
						if ( newIndex < oldIndex )
						{
							if ( pt.y < bounds.y + bounds.height / 3 )
							{
								newIndex = element.getIndex( );
							}
							else
								newIndex = element.getIndex( ) + 1;
						}
						else if ( newIndex > oldIndex )
						{
							if ( pt.y < bounds.y + bounds.height / 3 )
							{
								newIndex = element.getIndex( ) - 1;
							}
							else
								newIndex = element.getIndex( );
						}
						try
						{
							( (LevelHandle) obj ).moveTo( newIndex );
						}
						catch ( SemanticException e )
						{
							ExceptionHandler.handle( e );
						}

						groupViewer.expandToLevel( ( (LevelHandle) obj ),
								AbstractTreeViewer.ALL_LEVELS );
						groupViewer.setSelection( new StructuredSelection( ( (LevelHandle) obj ) ),
								true );
					}
				}
				refresh( );
			}
		} );

	}

	private void createMoveButtonsField( )
	{
		Composite buttonsField = new Composite( this, SWT.NONE );
		GridLayout layout = new GridLayout( );
		layout.marginWidth = 0;
		buttonsField.setLayout( layout );

		Button addButton = new Button( buttonsField, SWT.PUSH );

		addButton.addSelectionListener( new SelectionListener( ) {

			public void widgetDefaultSelected( SelectionEvent e )
			{

			}

			public void widgetSelected( SelectionEvent e )
			{
				handleDataAddEvent( );

			}

		} );

		Button removeButton = new Button( buttonsField, SWT.PUSH );

		removeButton.addSelectionListener( new SelectionListener( ) {

			public void widgetDefaultSelected( SelectionEvent e )
			{

			}

			public void widgetSelected( SelectionEvent e )
			{
				handleDelEvent( );
			}

		} );

		addButton.setText( ">" ); //$NON-NLS-1$
		removeButton.setText( "<" ); //$NON-NLS-1$

		GridData gd = new GridData( );
		gd.grabExcessVerticalSpace = true;
		gd.widthHint = Math.max( 25, addButton.computeSize( SWT.DEFAULT,
				SWT.DEFAULT ).x );
		gd.verticalAlignment = SWT.BOTTOM;
		addButton.setLayoutData( gd );

		gd = new GridData( );
		gd.widthHint = Math.max( 25, removeButton.computeSize( SWT.DEFAULT,
				SWT.DEFAULT ).x );
		gd.grabExcessVerticalSpace = true;
		gd.verticalAlignment = SWT.TOP;
		removeButton.setLayoutData( gd );

	}

	private void createDataField( )
	{
		Composite dataField = new Composite( this, SWT.NONE );
		dataField.setLayoutData( new GridData( GridData.FILL_BOTH ) );
		dataField.setLayout( new GridLayout( ) );

		Label dataLabel = new Label( dataField, SWT.NONE );
		dataLabel.setText( Messages.getString( "GroupsPage.Label.DataField" ) ); //$NON-NLS-1$
		dataFieldsViewer = new TreeViewer( dataField, SWT.SINGLE
				| SWT.H_SCROLL
				| SWT.V_SCROLL
				| SWT.BORDER );
		cubeLabelProvider = new CubeLabelProvider( );
		dataFieldsViewer.setLabelProvider( cubeLabelProvider );
		dataFieldsViewer.setContentProvider( dataContentProvider );
		dataFieldsViewer.setAutoExpandLevel( 3 );
		GridData gd = new GridData( GridData.FILL_BOTH );
		dataFieldsViewer.getTree( ).setLayoutData( gd );
		( (GridData) dataFieldsViewer.getTree( ).getLayoutData( ) ).heightHint = 250;
		( (GridData) dataFieldsViewer.getTree( ).getLayoutData( ) ).widthHint = 225;
		dataFieldsViewer.addSelectionChangedListener( new ISelectionChangedListener( ) {

			public void selectionChanged( SelectionChangedEvent event )
			{
				updateButtons( );
			}

		} );

		final DragSource fieldsSource = new DragSource( dataFieldsViewer.getTree( ),
				operations );
		fieldsSource.setTransfer( types );
		fieldsSource.addDragListener( new CustomDragListener( dataFieldsViewer ) );

	}

	private Button addBtn;
	private Button delBtn;
	private int operations = DND.DROP_MOVE | DND.DROP_COPY | DND.DROP_LINK;
	private Transfer[] types = new Transfer[]{
		TextTransfer.getInstance( )
	};
	private Button editBtn;

	public void load( )
	{
		if ( input != null )
		{
			if ( datasets[0] != null )
				dataFieldsViewer.setInput( datasets );
			else if ( input.getDataSet( ) != null )
			{
				cubeLabelProvider.setInput( input );
				dataFieldsViewer.setInput( input );
			}
			groupViewer.setInput( input );
			getListenerElementVisitor( ).addListener( input );
			updateButtons( );
		}
	}

	private ListenerElementVisitor visitor;

	private ListenerElementVisitor getListenerElementVisitor( )
	{
		if ( visitor == null )
		{
			visitor = new ListenerElementVisitor( this );
		}
		return visitor;
	}

	protected void updateButtons( )
	{
		groupViewer.refresh( );
		TreeSelection selections = (TreeSelection) groupViewer.getSelection( );
		if ( selections.size( ) == 1 )
		{
			Iterator iter = selections.iterator( );
			Object obj = iter.next( );

			TreeSelection dataSelection = (TreeSelection) dataFieldsViewer.getSelection( );
			ResultSetColumnHandle dataField = null;
			DataSetHandle dataset = null;
			if ( dataSelection.size( ) == 1
					&& dataSelection.getFirstElement( ) != null
					&& dataSelection.getFirstElement( ) instanceof ResultSetColumnHandle )
			{
				dataField = (ResultSetColumnHandle) dataSelection.getFirstElement( );
				dataset = (DataSetHandle) dataField.getElementHandle( );
			}

			/**
			 * Deal add button and del buuton.
			 */
			if ( obj instanceof DimensionHandle
					|| obj instanceof LevelHandle
					|| obj instanceof MeasureHandle
					|| obj instanceof MeasureGroupHandle
					|| obj instanceof VirtualField )
			{
				DimensionHandle dimenTemp = null;
				if ( obj instanceof DimensionHandle )
				{
					dimenTemp = ( (DimensionHandle) obj );
				}
				else if ( obj instanceof VirtualField
						&& ( (VirtualField) obj ).getType( )
								.equals( VirtualField.TYPE_LEVEL ) )
				{
					dimenTemp = (DimensionHandle) ( (VirtualField) obj ).getModel( );

				}
				else if ( obj instanceof LevelHandle )
				{
					DesignElementHandle hierarchy = ( (LevelHandle) obj ).getContainer( );
					dimenTemp = (TabularDimensionHandle) hierarchy.getContainer( );
				}
				else
					addBtn.setEnabled( true );

				if ( dimenTemp != null )
				{
					if ( dimenTemp.isTimeType( ) )
						addBtn.setEnabled( false );
					else
					{
						if ( dataField != null
								&& isDateType( dataField.getDataType( ) ) )
						{
							if ( dimenTemp.getDefaultHierarchy( )
									.getContentCount( IHierarchyModel.LEVELS_PROP ) > 0 )
							{
								addBtn.setEnabled( false );
							}
							else
								addBtn.setEnabled( true );
						}
						else
							addBtn.setEnabled( true );
					}
				}

				if ( dataField == null || dataset == null )
					addBtn.setEnabled( false );
				else if ( obj instanceof MeasureGroupHandle
						|| ( obj instanceof VirtualField && ( (VirtualField) obj ).getType( )
								.equals( VirtualField.TYPE_MEASURE ) )
						|| obj instanceof MeasureHandle )
				{
					if ( dataset == input.getDataSet( ) )
						addBtn.setEnabled( true );
					else
						addBtn.setEnabled( false );
				}

				if ( obj instanceof LevelHandle )
				{
					DimensionHandle dimension = (DimensionHandle) ( (LevelHandle) obj ).getContainer( )
							.getContainer( );
					if ( dimension.isTimeType( ) )
					{
						delBtn.setEnabled( true );
					}
					else
					{
						delBtn.setEnabled( true );
					}
				}
				else if ( obj instanceof VirtualField )
				{
					delBtn.setEnabled( false );
				}
				else
				{
					delBtn.setEnabled( true );
				}
			}
			else
			{
				delBtn.setEnabled( false );
			}

			/**
			 * CubeHandle can do nothing.
			 */
			if ( obj instanceof CubeHandle )
				addBtn.setEnabled( false );
			/**
			 * CubeModel can and a group or a summary field
			 */
			else if ( obj instanceof PropertyHandle )
				addBtn.setEnabled( true );

			/**
			 * Only Level Handle has EditBtn and PropBtn.
			 */
			if ( obj instanceof LevelHandle )
			{
				TabularLevelHandle level = (TabularLevelHandle) obj;
				if ( level != null
						&& level.attributesIterator( ) != null
						&& level.attributesIterator( ).hasNext( ) )
				{
					String name = level.getName( ) + " (" //$NON-NLS-1$
							+ level.getColumnName( )
							+ ": "; //$NON-NLS-1$
					Iterator attrIter = level.attributesIterator( );
					while ( attrIter.hasNext( ) )
					{
						name += ( (LevelAttributeHandle) attrIter.next( ) ).getName( );
						if ( attrIter.hasNext( ) )
							name += ", "; //$NON-NLS-1$
					}
					name += ")"; //$NON-NLS-1$
					groupViewer.getTree( ).getSelection( )[0].setText( name );
				}
				editBtn.setEnabled( true );
			}
			else
			{
				if ( obj instanceof DimensionHandle
						|| obj instanceof MeasureGroupHandle
						|| obj instanceof MeasureHandle )
					editBtn.setEnabled( true );
				else
					editBtn.setEnabled( false );
			}
		}
		else
		{
			addBtn.setEnabled( false );
			delBtn.setEnabled( false );
			editBtn.setEnabled( false );
		}

	}
	private DataContentProvider dataContentProvider = new DataContentProvider( );
	private CubeLabelProvider cubeLabelProvider;

	private void handleDelEvent( )
	{
		TreeSelection slections = (TreeSelection) groupViewer.getSelection( );
		Iterator iter = slections.iterator( );
		while ( iter.hasNext( ) )
		{
			Object obj = iter.next( );
			if ( obj instanceof DimensionHandle )
			{
				DimensionHandle dimension = (DimensionHandle) obj;

				boolean hasExecuted = OlapUtil.enableDrop( dimension );
				if ( hasExecuted )
				{
					UIHelper.dropDimensionProperties( dimension );
					new DeleteCommand( dimension ).execute( );
				}

			}
			else if ( obj instanceof LevelHandle )
			{
				LevelHandle level = (LevelHandle) obj;
				DesignElementHandle hierarchy = level.getContainer( );
				DimensionHandle dimension = (DimensionHandle) hierarchy.getContainer( );

				boolean hasExecuted = OlapUtil.enableDrop( level );
				if ( hasExecuted )
				{
					new DeleteCommand( level ).execute( );
				}
				try
				{
					if ( hierarchy.getContentCount( IHierarchyModel.LEVELS_PROP ) == 0 )
					{
						dimension.setTimeType( false );
					}
				}
				catch ( SemanticException e )
				{
					ExceptionHandler.handle( e );
				}

			}
			else if ( obj instanceof MeasureGroupHandle )
			{
				MeasureGroupHandle measureGroup = (MeasureGroupHandle) obj;
				boolean hasExecuted = OlapUtil.enableDrop( measureGroup );
				if ( hasExecuted )
				{
					new DeleteCommand( measureGroup ).execute( );
				}
			}
			else if ( obj instanceof MeasureHandle )
			{
				MeasureHandle measure = (MeasureHandle) obj;
				boolean hasExecuted = OlapUtil.enableDrop( measure );
				if ( hasExecuted )
				{
					new DeleteCommand( measure ).execute( );
				}
			}

		}
		updateButtons( );
	}

	private void handleAddEvent( )
	{
		TreeSelection slections = (TreeSelection) groupViewer.getSelection( );
		Iterator iter = slections.iterator( );
		while ( iter.hasNext( ) )
		{
			Object obj = iter.next( );

			TreeSelection dataFields = (TreeSelection) dataFieldsViewer.getSelection( );
			Iterator iterator = dataFields.iterator( );
			ResultSetColumnHandle dataField = null;
			while ( iterator.hasNext( ) )
			{
				Object temp = iterator.next( );
				if ( !( temp instanceof ResultSetColumnHandle ) )
					continue;
				dataField = (ResultSetColumnHandle) temp;
			}

			if ( dataField != null )
			{
				handleDataAddEvent( );
			}

			if ( obj instanceof PropertyHandle )
			{
				PropertyHandle model = (PropertyHandle) obj;
				if ( model.getPropertyDefn( )
						.getName( )
						.equals( ICubeModel.DIMENSIONS_PROP ) )
				{
					SessionHandleAdapter.getInstance( )
							.getCommandStack( )
							.startTrans( "" );
					DimensionHandle dimension = DesignElementFactory.getInstance( )
							.newTabularDimension( null );
					try
					{
						model.getElementHandle( )
								.add( ICubeModel.DIMENSIONS_PROP, dimension );
					}
					catch ( SemanticException e1 )
					{
						ExceptionHandler.handle( e1 );
					}

					RenameInputDialog inputDialog = new RenameInputDialog( getShell( ),
							Messages.getString( "CubeGroupContent.Group.Add.Title" ), //$NON-NLS-1$
							Messages.getString( "CubeGroupContent.Group.Add.Message" ), //$NON-NLS-1$
							( (DesignElementHandle) dimension ).getName( ),
							null );
					inputDialog.create( );
					if ( inputDialog.open( ) == Window.OK )
					{
						try
						{
							( (DesignElementHandle) dimension ).setName( inputDialog.getValue( )
									.trim( ) );
							SessionHandleAdapter.getInstance( )
									.getCommandStack( )
									.commit( );
						}
						catch ( NameException e1 )
						{
							SessionHandleAdapter.getInstance( )
									.getCommandStack( )
									.rollback( );
							ExceptionHandler.handle( e1 );
						}
					}
					else
					{
						SessionHandleAdapter.getInstance( )
								.getCommandStack( )
								.rollback( );
					}
					refresh( );
				}
				else if ( model.getPropertyDefn( )
						.getName( )
						.equals( ICubeModel.MEASURE_GROUPS_PROP ) )
				{
					SessionHandleAdapter.getInstance( )
							.getCommandStack( )
							.startTrans( "" );
					MeasureGroupHandle measureGroup = DesignElementFactory.getInstance( )
							.newTabularMeasureGroup( null );
					try
					{
						model.getElementHandle( )
								.add( ICubeModel.MEASURE_GROUPS_PROP,
										measureGroup );
						if ( model.getElementHandle( )
								.getContentCount( ICubeModel.MEASURE_GROUPS_PROP ) == 1 )
							( (CubeHandle) model.getElementHandle( ) ).setDefaultMeasureGroup( measureGroup );
					}
					catch ( SemanticException e1 )
					{
						ExceptionHandler.handle( e1 );
					}

					RenameInputDialog inputDialog = new RenameInputDialog( getShell( ),
							Messages.getString( "CubeGroupContent.Measure.Add.Title" ), //$NON-NLS-1$
							Messages.getString( "CubeGroupContent.Message.Add.Message" ), //$NON-NLS-1$
							( (DesignElementHandle) measureGroup ).getName( ),
							null );
					inputDialog.create( );
					if ( inputDialog.open( ) == Window.OK )
					{
						try
						{
							( (DesignElementHandle) measureGroup ).setName( inputDialog.getValue( )
									.trim( ) );
							SessionHandleAdapter.getInstance( )
									.getCommandStack( )
									.commit( );
						}
						catch ( NameException e1 )
						{
							SessionHandleAdapter.getInstance( )
									.getCommandStack( )
									.rollback( );
							ExceptionHandler.handle( e1 );
						}
					}
					else
					{
						SessionHandleAdapter.getInstance( )
								.getCommandStack( )
								.rollback( );
					}

					refresh( );
				}
			}
		}
	}

	private boolean isDateType( String dataType )
	{
		return dataType.equals( DesignChoiceConstants.COLUMN_DATA_TYPE_DATETIME )
				|| dataType.equals( DesignChoiceConstants.COLUMN_DATA_TYPE_DATE )
				|| dataType.equals( DesignChoiceConstants.COLUMN_DATA_TYPE_TIME );
	}

	protected void handleDataAddEvent( )
	{
		TreeSelection dataFields = (TreeSelection) dataFieldsViewer.getSelection( );
		Iterator iterator = dataFields.iterator( );
		while ( iterator.hasNext( ) )
		{
			Object temp = iterator.next( );
			if ( !( temp instanceof ResultSetColumnHandle ) )
				continue;

			ResultSetColumnHandle dataField = (ResultSetColumnHandle) temp;
			DataSetHandle dataset = (DataSetHandle) dataField.getElementHandle( );
			DataSetHandle primary = ( input ).getDataSet( );

			TreeSelection slections = (TreeSelection) groupViewer.getSelection( );
			Iterator iter = slections.iterator( );
			while ( iter.hasNext( ) )
			{
				Object obj = iter.next( );
				if ( obj instanceof TabularLevelHandle )
				{
					if ( isDateType( dataField.getDataType( ) ) )
						continue;

					TabularHierarchyHandle hierarchy = ( (TabularHierarchyHandle) ( (TabularLevelHandle) obj ).getContainer( ) );
					TabularDimensionHandle dimension = (TabularDimensionHandle) hierarchy.getContainer( );
					if ( dimension.isTimeType( ) )
						continue;

					DataSetHandle dasetTemp = hierarchy.getDataSet( );
					if ( dasetTemp != null
							&& dataset != null
							&& dataset != dasetTemp )
					{
						continue;
					}

					if ( hierarchy.getDataSet( ) == null )
					{
						try
						{
							hierarchy.setDataSet( dataset );
						}
						catch ( SemanticException e )
						{
							ExceptionHandler.handle( e );
						}
					}
					TabularLevelHandle level = DesignElementFactory.getInstance( )
							.newTabularLevel( dimension,
									dataField.getColumnName( ) );
					try
					{
						level.setColumnName( dataField.getColumnName( ) );
						level.setDataType( dataField.getDataType( ) );
						( (TabularLevelHandle) obj ).getContainer( )
								.add( IHierarchyModel.LEVELS_PROP,
										level,
										( (TabularLevelHandle) obj ).getIndex( ) + 1 );
					}
					catch ( SemanticException e )
					{
						ExceptionHandler.handle( e );
					}
					LevelPropertyDialog dialog = new LevelPropertyDialog( true );
					dialog.setInput( level );
					dialog.open( );
					refresh( );
					return;
				}
				else if ( obj instanceof DimensionHandle
						|| ( obj instanceof VirtualField && ( (VirtualField) obj ).getType( )
								.equals( VirtualField.TYPE_LEVEL ) ) )
				{
					TabularDimensionHandle dimension = null;
					if ( obj instanceof TabularDimensionHandle )
						dimension = (TabularDimensionHandle) obj;
					else
						dimension = (TabularDimensionHandle) ( (VirtualField) obj ).getModel( );
					if ( dimension.isTimeType( ) )
						continue;

					TabularHierarchyHandle hierarchy = (TabularHierarchyHandle) dimension.getContent( IDimensionModel.HIERARCHIES_PROP,
							0 );

					DataSetHandle dasetTemp = hierarchy.getDataSet( );
					if ( dasetTemp != null
							&& dataset != null
							&& dataset != dasetTemp )
					{
						continue;
					}

					if ( hierarchy.getDataSet( ) == null )
					{
						try
						{
							hierarchy.setDataSet( dataset );

						}
						catch ( SemanticException e )
						{
							ExceptionHandler.handle( e );
						}
					}
					try
					{
						if ( isDateType( dataField.getDataType( ) ) )
						{
							if ( hierarchy.getContentCount( IHierarchyModel.LEVELS_PROP ) > 0 )
							{
								continue;
							}
							else
							{
								DateGroupDialog dialog = new DateGroupDialog( );
								dialog.setInput( hierarchy,
										dataField.getColumnName( ) );
								if ( dialog.open( ) == Window.OK )
								{
									dimension.setTimeType( true );
								}
								else
								{
									boolean hasExecuted = OlapUtil.enableDrop( dimension );
									if ( hasExecuted )
									{
										UIHelper.dropDimensionProperties( dimension );
										new DeleteCommand( dimension ).execute( );
										refresh( );
										return;
									}
								}
							}
						}
						else
						{
							TabularLevelHandle level = DesignElementFactory.getInstance( )
									.newTabularLevel( dimension,
											dataField.getColumnName( ) );
							level.setColumnName( dataField.getColumnName( ) );
							level.setDataType( dataField.getDataType( ) );
							hierarchy.add( IHierarchyModel.LEVELS_PROP, level );
							LevelPropertyDialog dialog = new LevelPropertyDialog( true );
							dialog.setInput( level );
							dialog.open( );
						}

						if ( dataset != input.getDataSet( ) )
						{
							builder.showSelectionPage( builder.getLinkGroupNode( ) );
						}
					}
					catch ( SemanticException e )
					{
						ExceptionHandler.handle( e );
					}
					refresh( );
					return;
				}
				else if ( dataset != null
						&& primary != null
						&& dataset == primary )
				{
					if ( obj instanceof MeasureGroupHandle
							|| ( obj instanceof VirtualField && ( (VirtualField) obj ).getType( )
									.equals( VirtualField.TYPE_MEASURE ) ) )
					{
						MeasureGroupHandle measureGroup = null;
						if ( obj instanceof MeasureGroupHandle )
							measureGroup = (MeasureGroupHandle) obj;
						else
							measureGroup = (MeasureGroupHandle) ( (VirtualField) obj ).getModel( );

						TabularMeasureHandle measure = DesignElementFactory.getInstance( )
								.newTabularMeasure( dataField.getColumnName( ) );
						try
						{
							measure.setMeasureExpression( DEUtil.getExpression( dataField ) );
							measure.setDataType( dataField.getDataType( ) );
							measureGroup.add( IMeasureGroupModel.MEASURES_PROP,
									measure );
							MeasureDialog dialog = new MeasureDialog( false );
							dialog.setInput( input, measure );
							dialog.open( );
						}
						catch ( SemanticException e1 )
						{
							ExceptionHandler.handle( e1 );
						}
						refresh( );
						return;
					}
					else if ( obj instanceof MeasureHandle )
					{
						TabularMeasureHandle measure = DesignElementFactory.getInstance( )
								.newTabularMeasure( dataField.getColumnName( ) );
						try
						{
							measure.setMeasureExpression( DEUtil.getExpression( dataField ) );
							measure.setDataType( dataField.getDataType( ) );
							( (MeasureHandle) obj ).getContainer( )
									.add( IMeasureGroupModel.MEASURES_PROP,
											measure );
							MeasureDialog dialog = new MeasureDialog( false );
							dialog.setInput( input, measure );
							dialog.open( );
						}
						catch ( SemanticException e1 )
						{
							ExceptionHandler.handle( e1 );
						}
						refresh( );
						return;
					}
				}

			}
		}
	}

	public void refresh( )
	{
		updateButtons( );
	}

	public void elementChanged( DesignElementHandle focus, NotificationEvent ev )
	{
		if ( groupViewer == null || groupViewer.getControl( ).isDisposed( ) )
		{
			return;
		}
		groupViewer.refresh( );
		expandNodeAfterCreation( ev );
		getListenerElementVisitor( ).addListener( focus );

	}

	private void expandNodeAfterCreation( NotificationEvent ev )
	{
		if ( ev instanceof ContentEvent
				&& ev.getEventType( ) == NotificationEvent.CONTENT_EVENT
				&& ( (ContentEvent) ev ).getAction( ) == ContentEvent.ADD )
		{
			IDesignElement element = ( (ContentEvent) ev ).getContent( );
			if ( element != null )
			{
				final DesignElementHandle handle = element.getHandle( input.getModule( ) );
				groupViewer.expandToLevel( handle,
						AbstractTreeViewer.ALL_LEVELS );
				groupViewer.setSelection( new StructuredSelection( handle ),
						true );
				updateButtons( );
			}
		}
	}

	private void handleEditEvent( )
	{
		TreeSelection slections = (TreeSelection) groupViewer.getSelection( );
		Iterator iter = slections.iterator( );
		while ( iter.hasNext( ) )
		{
			Object obj = iter.next( );

			if ( obj instanceof TabularLevelHandle )
			{
				TabularLevelHandle level = (TabularLevelHandle) obj;
				if ( level.getDataType( )
						.equals( DesignChoiceConstants.COLUMN_DATA_TYPE_DATETIME ) )
				{
					DateLevelDialog dialog = new DateLevelDialog( );
					dialog.setInput( level );
					if ( dialog.open( ) == Window.OK )
					{
						refresh( );
					};
				}
				else
				{
					LevelPropertyDialog dialog = new LevelPropertyDialog( false );
					dialog.setInput( level );
					if ( dialog.open( ) == Window.OK )
					{
						refresh( );
					};
				}
			}
			else if ( obj instanceof TabularMeasureHandle )
			{
				TabularMeasureHandle level = (TabularMeasureHandle) obj;
				MeasureDialog dialog = new MeasureDialog( false );
				dialog.setInput( input, level );
				if ( dialog.open( ) == Window.OK )
				{
					refresh( );
				};
			}
			else if ( obj instanceof DimensionHandle
					&& ( (DimensionHandle) obj ).getDefaultHierarchy( ) != null
					&& ( (DimensionHandle) obj ).isTimeType( )
					&& ( (DimensionHandle) obj ).getDefaultHierarchy( )
							.getContentCount( IHierarchyModel.LEVELS_PROP ) > 0 )
			{
				DateGroupDialog dialog = new DateGroupDialog( );
				dialog.setInput( (TabularHierarchyHandle) ( (DimensionHandle) obj ).getDefaultHierarchy( ) );
				dialog.open( );
			}
			else
			{
				String title = Messages.getString( "RenameInputDialog.DialogTitle" ); //$NON-NLS-1$
				String message = Messages.getString( "RenameInputDialog.DialogMessage" ); //$NON-NLS-1$
				if ( obj instanceof DimensionHandle )
				{
					title = Messages.getString( "CubeGroupContent.Group.Edit.Title" ); //$NON-NLS-1$
					message = Messages.getString( "CubeGroupContent.Group.Edit.Message" ); //$NON-NLS-1$
				}
				else if ( obj instanceof MeasureGroupHandle )
				{
					title = Messages.getString( "CubeGroupContent.Measure.Edit.Title" ); //$NON-NLS-1$
					message = Messages.getString( "CubeGroupContent.Measure.Edit.Message" ); //$NON-NLS-1$
				}
				RenameInputDialog inputDialog = new RenameInputDialog( getShell( ),
						title,
						message,
						( (DesignElementHandle) obj ).getName( ),
						null );
				inputDialog.create( );
				if ( inputDialog.open( ) == Window.OK )
				{
					try
					{
						( (DesignElementHandle) obj ).setName( inputDialog.getValue( )
								.trim( ) );
					}
					catch ( NameException e1 )
					{
						ExceptionHandler.handle( e1 );
					}
				}
			}
		}
		updateButtons( );
	}
}
