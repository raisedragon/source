package org.adempiere.webui.apps.form;

import java.util.Vector;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.webui.component.ConfirmPanel;
import org.adempiere.webui.component.ListboxFactory;
import org.adempiere.webui.component.Panel;
import org.adempiere.webui.component.WListbox;
import org.adempiere.webui.component.Window;
import org.adempiere.webui.editor.WButtonEditor;
import org.adempiere.webui.panel.StatusBarPanel;
import org.compiere.minigrid.ColumnInfo;
import org.compiere.model.GridTab;
import org.compiere.model.MBExceptionDetail;
import org.compiere.model.MOrder;
import org.compiere.model.MWorkOrder;
import org.compiere.model.MWorkOrderLine;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zkex.zul.Borderlayout;
import org.zkoss.zkex.zul.Center;
import org.zkoss.zkex.zul.South;

public class WOrderCompletedTips extends Window implements EventListener,IExceptionTips
{

	/**
	 * 
	 */
	private static final long	serialVersionUID	= 883114344266765693L;
	/** Tab */
	private GridTab				m_mTab;
	private boolean				m_initOK			= false;
	/** Logger */
	private static CLogger		log					= CLogger.getCLogger(WExceptionTips.class);
	private Panel				mainPanel			= new Panel();
	private Borderlayout		mainLayout			= new Borderlayout();
	private WListbox			xTable				= ListboxFactory.newDataTable();
	private ConfirmPanel		confirmPanel		= new ConfirmPanel(true);

	private String				errorMessage		= "";
	
	public void setErrorMessage(String errorMessage)
	{
		this.errorMessage = errorMessage;
	}

	public WOrderCompletedTips(int WindowNo, GridTab mTab, WButtonEditor button)
	{
		super();
		this.setTitle(Msg.getMsg(Env.getCtx(), "WT_CantComplete"));
		this.setAttribute("mode", "modal");
		m_mTab = mTab;
		try
		{
			zkInit();
			m_initOK = dynInit(button); // Null Pointer if order/invoice not
										// saved yet
		}
		catch (Exception ex)
		{
			setErrorMessage(ex.getLocalizedMessage());
			log.log(Level.SEVERE, "ExceptionTips", ex);
			m_initOK = false;
		}
		//
		this.setHeight("400px");
		this.setWidth("600px");
		this.setBorder("normal");
	}

	private void zkInit() throws Exception
	{
		this.appendChild(mainPanel);
		mainPanel.appendChild(mainLayout);
		mainPanel.setStyle("width: 100%; height: 100%; padding: 0; margin: 0");
		mainLayout.setHeight("100%");
		mainLayout.setWidth("100%");
		Center center = new Center();
		mainLayout.appendChild(center);
		Panel p = new Panel();
		p.appendChild(xTable);
		xTable.setWidth("100%");
		xTable.setHeight("100%");
		p.setStyle("width: 100%; height: 100%; padding: 0; margin: 0");
		center.appendChild(p);
		//
		South south = new South();
		south.setStyle("border: none");
		mainLayout.appendChild(south);
		south.appendChild(confirmPanel);
		confirmPanel.addActionListener(this);
	} // jbInit

	private boolean dynInit(WButtonEditor button) throws Exception
	{
		// 获取海外入库单产生的异常
		Vector<Vector> data = getBExceptionData();
		if (data.size() <= 0)
		{
			// 当卖家海外仓入库单内所有单品状态都变更为"海外仓上架完成"时，将卖家海外仓入库单状态更新为"海外仓入库完成"
			if (m_mTab.getAD_Table_ID() == MOrder.Table_ID)
			{
				MOrder order = new MOrder(Env.getCtx(), m_mTab.getRecord_ID(), null);
				order.setStatus(MOrder.STATUS_ForeignWarehouseInboundCompleted);
				order.saveEx();
			}
			else if (m_mTab.getAD_Table_ID() == MWorkOrder.Table_ID)
			{
				MWorkOrder order = new MWorkOrder(Env.getCtx(), m_mTab.getRecord_ID(), null);
				order.setStatus(MWorkOrder.STATUS_Completed);
				order.saveEx();
			}

			// 不显示业务异常提示窗口
			return false;
		}

		ColumnInfo[] layout = new ColumnInfo[] {
				new ColumnInfo(Msg.translate(Env.getCtx(), "WT_ExceptionType"), ".", String.class),
				new ColumnInfo(Msg.translate(Env.getCtx(), "WT_ExceptionID"), ".", String.class) };
		xTable.prepareTable(layout, "", "", false, "");
		xTable.loadTable(data);
		// 显示业务异常提示窗口
		return true;
	} // dynInit

	@Override
	public void onEvent(Event e)
	{
		// Finish
		if (e.getTarget().getId().equals(ConfirmPanel.A_OK))
		{
			dispose();
		}
		else if (e.getTarget().getId().equals(ConfirmPanel.A_CANCEL))
			dispose();

	}

	private Vector<Vector> getBExceptionData()
	{
		Vector<Vector> data = new Vector<Vector>();

		final String whereClause = MBExceptionDetail.COLUMNNAME_Record_ID + " = " + m_mTab.getRecord_ID() + " AND "
				+ MBExceptionDetail.COLUMNNAME_Status + "<>" + DB.TO_STRING(MBExceptionDetail.STATUS_Completed);
		MBExceptionDetail[] bExceptionDetails = MBExceptionDetail.getBExceptionDetail(Env.getCtx(), whereClause);
		for (MBExceptionDetail ex : bExceptionDetails)
		{

			Vector<String> row = new Vector<String>();
			row.add(ex.getWT_BExceptionType().getName());
			row.add(ex.getValue());
			data.add(row);
		}

		return data;
	}

	public boolean isInitOK()
	{
		return m_initOK;
	} // isInitOK

	@Override
	public boolean needSave()
	{
		return true;
	}

	@Override
	public String getErrorMessage()
	{
 		return this.errorMessage;
	}
	
	
}
