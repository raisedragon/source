/******************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution * Copyright (C)
 * 1999-2006 ComPiere, Inc. All Rights Reserved. * This program is free
 * software; you can redistribute it and/or modify it * under the terms version
 * 2 of the GNU General Public License as published * by the Free Software
 * Foundation. This program is distributed in the hope * that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied * warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. * See the GNU General
 * Public License for more details. * You should have received a copy of the GNU
 * General Public License along * with this program; if not, write to the Free
 * Software Foundation, Inc., * 59 Temple Place, Suite 330, Boston, MA
 * 02111-1307 USA. * For the text or an alternative of this public license, you
 * may reach us * ComPiere, Inc., 2620 Augustine Dr. #245, Santa Clara, CA
 * 95054, USA * or via info@compiere.org or http://www.compiere.org/license.html
 * *
 *****************************************************************************/
package org.adempiere.webui.apps.form;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.List;
import java.util.Vector;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.webui.apps.BusyDialog;
import org.adempiere.webui.component.ConfirmPanel;
import org.adempiere.webui.component.Label;
import org.adempiere.webui.component.ListboxFactory;
import org.adempiere.webui.component.Panel;
import org.adempiere.webui.component.Textbox;
import org.adempiere.webui.component.WListbox;
import org.adempiere.webui.component.Window;
import org.adempiere.webui.editor.WButtonEditor;
import org.adempiere.webui.window.FDialog;
import org.compiere.minigrid.ColumnInfo;
import org.compiere.minigrid.IDColumn;
import org.compiere.model.GridTab;
import org.compiere.model.I_M_Locator;
import org.compiere.model.MBExceptionDetail;
import org.compiere.model.MLocator;
import org.compiere.model.MPInstance;
import org.compiere.model.MPayment;
import org.compiere.model.MProcess;
import org.compiere.model.MRole;
import org.compiere.model.MTTaskGroup;
import org.compiere.model.MTTaskGroupInspection;
import org.compiere.model.MTTaskGroupMovement;
import org.compiere.model.MTTaskGroupOutbound;
import org.compiere.model.MTTaskGroupSealing;
import org.compiere.model.MTTaskGroupShelves_OWMS;
import org.compiere.model.MTTaskGroupUnload;
import org.compiere.model.MTTaskGroupUnloadOWMS;
import org.compiere.model.MTTaskGroupUnpacking;
import org.compiere.model.MTTaskType;
import org.compiere.model.Query;
import org.compiere.model.X_WT_OFC;
import org.compiere.process.ProcessInfo;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.KeyNamePair;
import org.compiere.util.Msg;
import org.compiere.util.Trx;
import org.compiere.util.TrxRunnable;
import org.compiere.www.WProcess;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zkex.zul.Borderlayout;
import org.zkoss.zkex.zul.Center;
import org.zkoss.zkex.zul.North;
import org.zkoss.zkex.zul.South;
import org.zkoss.zul.Space;

/**
 * Display (and process) Payment Options.
 * 
 * <pre>
 *  Payment Rule
 *  -B- Cash          (Date)          -> Cash Entry
 *  -P- Payment Term  (Term)
 *  -S- Check         (Routing, ..)   -> Payment Entry
 *  -K- CreditCard    (No)            -> Payment Entry
 *  -U- ACH Transfer  (Routing)       -> Payment Entry
 * 
 *  When processing:
 *  - If an invoice is a S/K/U, but has no Payment Entry, it is changed to P
 *  - If an invoive is B and has no Cash Entry, it is created
 *  - An invoice is "Open" if it is "P" and no Payment
 * 
 *  Entry:
 *  - If not processed, an invoice has no Cash or Payment entry
 *  - The entry is created, during "Online" and when Saving
 * 
 *  Changes/Reversals:
 *  - existing Cash Entries are reversed and newly created
 *  - existing Payment Entries are not changed and then "hang there" and need to be allocated
 * </pre>
 * 
 * @author Jorg Janke
 * @version $Id: VPayment.java,v 1.2 2006/07/30 00:51:28 jjanke Exp $
 * @author Teo Sarca, SC ARHIPAC SERVICE SRL <li>BF [ 1763488 ] Error on cash
 *         payment <li>BF [ 1789949 ] VPayment: is displaying just
 *         "CashNotCreated"
 */
public class WExceptionTips extends Window implements EventListener,IExceptionTips
{
	/**
	 * 
	 */
	private static final long	serialVersionUID	= 3550713503274155601L;

	/**
	 * Constructor
	 * 
	 * @param WindowNo owning window
	 * @param mTab owning tab
	 * @param button button with access information
	 */
	public WExceptionTips(int WindowNo, GridTab mTab, WButtonEditor button)
	{
		super();
		this.setTitle(Msg.getMsg(Env.getCtx(), "WT_ExceptionTips"));
		this.setAttribute("mode", "modal");
		m_WindowNo = WindowNo;
		m_mTab = mTab;
		try
		{
			zkInit();
			m_initOK = dynInit(button); // Null Pointer if order/invoice not
										// saved yet
		}
		catch (Exception ex)
		{
			log.log(Level.SEVERE, "ExceptionTips", ex);
			FDialog.error(m_WindowNo, this, ex.getLocalizedMessage());
			m_initOK = false;
		}
		//
		this.setHeight("400px");
		this.setWidth("600px");
		this.setBorder("normal");
	} // VPayment

	public WExceptionTips()
	{
		// TODO Auto-generated constructor stub
	}

	/** Window */
	private int				m_WindowNo		= 0;
	/** Tab */
	private GridTab			m_mTab;

	private MPayment		m_mPayment		= null;

	//
	private boolean			m_initOK		= false;
	/** Only allow changing Rule */

	private boolean			m_needSave		= false;
	/** Logger */
	private static CLogger	log				= CLogger.getCLogger(WExceptionTips.class);

	//
	private Panel			mainPanel		= new Panel();
	private Borderlayout	mainLayout		= new Borderlayout();
	private Panel			northPanel		= new Panel();
	private Label			eLabel			= new Label();
	private Textbox			eText			= new Textbox();
	private WListbox		xTable			= ListboxFactory.newDataTable();
	private ConfirmPanel	confirmPanel	= new ConfirmPanel(true);

	private boolean			m_isLocked		= false;
	private BusyDialog		progressWindow;

	/**
	 * Static Init
	 * 
	 * @throws Exception
	 */
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
		eLabel.setText(Msg.translate(Env.getCtx(), "WT_DispatcherMess"));
		North north = new North();
		north.setStyle("border: none");
		mainLayout.appendChild(north);
		north.appendChild(northPanel);
		northPanel.appendChild(eLabel);
		northPanel.appendChild(new Space());
		northPanel.appendChild(eText);
		eText.setCols(2);
		eText.setWidth("80%");
		//
		South south = new South();
		south.setStyle("border: none");
		mainLayout.appendChild(south);
		south.appendChild(confirmPanel);
		confirmPanel.addActionListener(this);
	} // jbInit

	/**************************************************************************
	 * Dynamic Init. B (Cash) (Currency) K (CreditCard) Type, Number, Exp,
	 * Approval L (DirectDebit) BPartner_Bank P (PaymentTerm) PaymentTerm S
	 * (Check) (Currency) CheckNo, Routing Currencies are shown, if member of
	 * EMU
	 * 
	 * @param button payment type button
	 * @return true if init OK
	 * @throws Exception
	 */
	private boolean dynInit(WButtonEditor button) throws Exception
	{
		// if (m_mTab.getValue("C_BPartner_ID") == null)
		// {
		// FDialog.error(0, this, "SaveErrorRowNotFound");
		// return false;
		// }

		// 获猎取任务步骤产生的业务异常和任务组产生的异常
		Vector<Vector> data = getBExceptionData();
		if (data.size() <= 0)
		{
			// 完成任务组
			MTTaskGroup taskGroup = new MTTaskGroup(Env.getCtx(), m_mTab.getRecord_ID(), null);
			taskGroup.setDispatcherMess(null == eText.getText() ? "" : eText.getText());
			taskGroup.setStatus(MTTaskGroup.STATUS_Completed);
			taskGroup.saveEx();
			m_needSave = true;
			return false;
		}

		ColumnInfo[] layout = new ColumnInfo[] { new ColumnInfo(" ", ".", IDColumn.class, true, false, ""),
				new ColumnInfo(Msg.translate(Env.getCtx(), "WT_ExceptionType"), ".", String.class),
				new ColumnInfo(Msg.translate(Env.getCtx(), "WT_ExceptionDescription"), ".", String.class), };
		xTable.prepareTable(layout, "", "", false, "");
		xTable.loadTable(data);
		return true;
	} // dynInit

	// 获取任务组异常
	private Vector<Vector> getTaskGroupEx()
	{
		Vector<Vector> data = new Vector();
		MTTaskGroup tg = new MTTaskGroup(Env.getCtx(), m_mTab.getRecord_ID(), null);
		String typeCode = tg.getWT_T_TaskType().getTaskTypeCode();
		if (typeCode.equals(MTTaskType.UNLOAD_CODE))
		{
			MTTaskGroupUnload unLoad = new MTTaskGroupUnload(tg);
			data = unLoad.getTaskGroupEx();
		}
		else if (typeCode.equals(MTTaskType.MOVEMENT_CODE))
		{
			MTTaskGroupMovement Movement = new MTTaskGroupMovement(tg);
			data = Movement.getTaskGroupEx();
		}
		else if (typeCode.equals(MTTaskType.UNPACKING_CODE))
		{
			MTTaskGroupUnpacking Unpacking = new MTTaskGroupUnpacking(tg);
			data = Unpacking.getTaskGroupEx();
		}
		else if (typeCode.equals(MTTaskType.INSPECTION_CODE))
		{
			MTTaskGroupInspection Inspection = new MTTaskGroupInspection(tg);
			data = Inspection.getTaskGroupEx();
		}
		else if (typeCode.equals(MTTaskType.SEALING_CODE))
		{
			MTTaskGroupSealing Sealing = new MTTaskGroupSealing(tg);
			data = Sealing.getTaskGroupEx();
		}
		else if (typeCode.equals(MTTaskType.OUTBOUND_CODE))
		{
			MTTaskGroupOutbound Outbound = new MTTaskGroupOutbound(tg);
			data = Outbound.getTaskGroupEx();
		}
		else if (typeCode.equals(MTTaskType.UNLOAD_OWMS_CODE))
		{
			MTTaskGroupUnloadOWMS UnloadOwms = new MTTaskGroupUnloadOWMS(tg);
			data = UnloadOwms.getTaskGroupEx();
		}
		else if(typeCode.equals(MTTaskType.OWMS_SHELVES_CODE))
		{
			//上架任务组类型为直发单不需要产生异常！
			if (!tg.getWT_OFC().getReceiptType().equals(X_WT_OFC.RECEIPTTYPE_DirectSend))
			{
				MTTaskGroupShelves_OWMS ShelvesOwms = new MTTaskGroupShelves_OWMS(tg);
				data = ShelvesOwms.getTaskGroupEx();
			}
		}
		return data;
	}

	// 任务步骤产生的业务异常和任务组产生的异常
	private Vector<Vector> getBExceptionData()
	{
		// 任务组产生的异常
		Vector<Vector> data = getTaskGroupEx();
		// 任务步骤产生的业务异常
		final String whereClause = MBExceptionDetail.COLUMNNAME_WT_T_TaskGroup_ID + " = " + m_mTab.getRecord_ID();
		MBExceptionDetail[] BExceptionDetails = MBExceptionDetail.getBExceptionDetail(Env.getCtx(), whereClause);
		if (null != BExceptionDetails && BExceptionDetails.length > 0)
		{
			for (MBExceptionDetail ex : BExceptionDetails)
			{
				Vector row = new Vector();
				row.add(ex.getRecord_ID());
				row.add(ex.getWT_BExceptionType().getName());
				row.add(ex.getDescription());
				data.add(row);
			}
		}
		return data;
	} // saveChanges

	/**
	 * Init OK to be able to make changes?
	 * 
	 * @return true if init OK
	 */
	public boolean isInitOK()
	{
		return m_initOK;
	} // isInitOK

	/**************************************************************************
	 * Action Listener
	 * 
	 * @param e event
	 */
	public void onEvent(Event e)
	{
		// Finish
		if (e.getTarget().getId().equals(ConfirmPanel.A_OK))
		{
			saveChanges(); // cannot recover
			dispose();
		}
		else if (e.getTarget().getId().equals(ConfirmPanel.A_CANCEL))
			dispose();
	} // actionPerformed

	public void lockUI()
	{
		if (m_isLocked)
			return;

		m_isLocked = true;

		showBusyDialog();
	}

	private void showBusyDialog()
	{
		progressWindow = new BusyDialog();
		progressWindow.setPage(this.getPage());
		progressWindow.doHighlighted();
	}

	public void runProcessOnline()
	{
		try
		{
			processOnline();
		}
		finally
		{
			unlockUI();
		}
	}

	public void unlockUI()
	{
		if (!m_isLocked)
			return;

		m_isLocked = false;
		hideBusyDialog();
		updateUI();
	}

	private void hideBusyDialog()
	{
		if (progressWindow != null)
		{
			progressWindow.dispose();
			progressWindow = null;
		}
	}

	private void updateUI()
	{
		if (m_mPayment.isApproved())
			dispose();
	}

	/**************************************************************************
	 * Save Changes
	 * 
	 * @return true, if Window can exit
	 */
	private boolean saveChanges()
	{

		// BF [ 1920179 ] perform the save in a trx's context.
		final boolean[] success = new boolean[] { false };
		final TrxRunnable r = new TrxRunnable() {

			public void run(String trxName)
			{
				success[0] = saveChangesInTrx(trxName);
			}
		};
		try
		{
			Trx.run(r);
		}
		catch (Throwable e)
		{
			success[0] = false;
			FDialog.error(m_WindowNo, this, "WT_BExceptionError", e.getLocalizedMessage());
		}
		return success[0];
	} // saveChanges

	/**************************************************************************
	 * Save Changes
	 * 
	 * @return true, if eindow can exit
	 */
	private boolean saveChangesInTrx(final String trxName)
	{
		String DispatcherMess = null == eText.getText() ? "" : eText.getText();
		MTTaskGroup taskGroup = new MTTaskGroup(Env.getCtx(), m_mTab.getRecord_ID(), trxName);
		if (taskGroup.getWT_OFC_ID() == 0)
		{
			// 生成业务异常
			taskGroup.createBException(trxName);
		}
		else
		{
			if (!taskGroup.getWT_OFC().getReceiptType().equals(X_WT_OFC.RECEIPTTYPE_DirectSend))
			{
				// 生成业务异常
				taskGroup.createBException(trxName);
			}
		}
		
		// BUG #712 无论是扫描枪或者是任务产生的异常，都要将调度留言保存在相应异常记录中
		// 更新调度留言到任务步骤产生的异常中
		final String whereClause = MBExceptionDetail.COLUMNNAME_WT_T_TaskGroup_ID + " = ? ";
		List<MBExceptionDetail> lists = new Query(Env.getCtx(), MBExceptionDetail.Table_Name, whereClause, trxName)
				.setParameters(m_mTab.getRecord_ID()).list();
		if (null != lists && lists.size() > 0)
		{
			for (MBExceptionDetail list : lists)
			{
				list.setDispatcherMess(DispatcherMess);
				list.saveEx();
			}
		}
		
		// 完成任务组
		taskGroup.setDispatcherMess(DispatcherMess);
		taskGroup.setStatus(MTTaskGroup.STATUS_Completed);
		taskGroup.saveEx();
		m_needSave = true;
		return true;
	} // saveChanges

	/*
	 * private boolean callProcess(String provessValue, ProcessInfoParameter[]
	 * pp, final String trxName) throws Exception { try { MProcess process = new
	 * Query(Env.getCtx(), MProcess.Table_Name, "value=?",
	 * trxName).setParameters( new Object[] { provessValue }).first(); if
	 * (process == null) { throw new AdempiereException("@ProcessNotExist@"); }
	 * // add parameters. MPInstance mpi = new MPInstance(Env.getCtx(), 0,
	 * trxName); mpi.setAD_Process_ID(process.get_ID());
	 * mpi.setRecord_ID(m_mTab.getRecord_ID()); mpi.save(); ProcessInfo pi = new
	 * ProcessInfo(provessValue, process.getAD_Process_ID(), 0, 0); if
	 * (process.isReport()) pi.setPrintPreview(true); if (pp != null)
	 * pi.setParameter(pp); pi.setAD_PInstance_ID(mpi.get_ID());
	 * pi.setRecord_ID(m_mTab.getRecord_ID()); return process.processIt(pi,
	 * null); } catch (Exception e) { System.out.println(e.getMessage()); }
	 * return true; }
	 */

	/**************************************************************************
	 * Process Online (sales only) - if approved - exit
	 */
	private void processOnline()
	{
		log.config("");
	} // online

	/**
	 * Need Save record (payment with waiting order)
	 * 
	 * @return true if payment with waiting order
	 */
	public boolean needSave()
	{
		return m_needSave;
	} // needSave

	@Override
	public String getErrorMessage()
	{
		return "";
	}

} // VPayment
