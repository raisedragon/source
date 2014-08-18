package org.adempiere.webui.apps.form;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.webui.apps.BusyDialog;
import org.adempiere.webui.component.ConfirmPanel;
import org.adempiere.webui.component.Label;
import org.adempiere.webui.component.ListboxFactory;
import org.adempiere.webui.component.Panel;
import org.adempiere.webui.component.Searchbox;
import org.adempiere.webui.component.Textbox;
import org.adempiere.webui.component.WListbox;
import org.adempiere.webui.component.Window;
import org.adempiere.webui.editor.WButtonEditor;
import org.adempiere.webui.window.FDialog;
import org.apache.commons.lang.StringUtils;
import org.compiere.minigrid.ColumnInfo;
import org.compiere.minigrid.IDColumn;
import org.compiere.minigrid.IMiniTable;
import org.compiere.model.GridTab;
import org.compiere.model.MOFCDetail;
import org.compiere.model.MOFC;
import org.compiere.model.MPayment;
import org.compiere.model.Query;
import org.compiere.model.X_WT_OFC;
import org.compiere.model.X_WT_OFCDetail;
import org.compiere.model.X_WT_OWMS_Shipment;
import org.compiere.model.X_WT_Receipt;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.Trx;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zkex.zul.Borderlayout;
import org.zkoss.zkex.zul.Center;
import org.zkoss.zkex.zul.North;
import org.zkoss.zkex.zul.South;
import org.zkoss.zul.Space;

/**
 * 生成ofc收货单
 * 
 * @author temuser3 201309
 */
public class CreateFormOFCDetail extends Window implements EventListener,
		IExceptionTips {
	/**
	 * 
	 */
	private static final long serialVersionUID = 4668156590280281473L;

	/** 存放分区 */
	public Object m_Warehousebonded_Id = null;

	/** Window */
	private int m_WindowNo = 0;
	/** Tab */
	private GridTab m_mTab;

	private MPayment m_mPayment = null;

	//
	private boolean m_initOK = false;
	/** Only allow changing Rule */

	private boolean m_needSave = false;
	/** Logger */
	private static CLogger log = CLogger.getCLogger(CreateFormOFCDetail.class);

	//
	private Panel mainPanel = new Panel();
	private Borderlayout mainLayout = new Borderlayout();
	private Panel northPanel = new Panel();
	private Label eLabel = new Label();
	private Textbox eText = new Textbox();
	private WListbox xTable = ListboxFactory.newDataTable();
	private ConfirmPanel confirmPanel = new ConfirmPanel(true, true, false,
			false, false, false);

	private boolean m_isLocked = false;
	private BusyDialog progressWindow;

	private String currentReceiptType;// 当前OFC收货单单据类型
	private X_WT_OFC ofc;

	public CreateFormOFCDetail(int WindowNo, GridTab mTab, WButtonEditor button) {
		super();
		this.setTitle(Msg.getMsg(Env.getCtx(), "WT_CreateOFCDetail"));
		this.setAttribute("mode", "modal");
		m_WindowNo = WindowNo;
		m_mTab = mTab; 
		try {
			ofc = new X_WT_OFC(Env.getCtx(), m_mTab.getRecord_ID(),null);
			currentReceiptType =  ofc.getReceiptType();
			
			zkInit();
			m_initOK = dynInit(button); // Null Pointer if order/invoice not
										// saved yet

		} catch (Exception ex) {
			log.log(Level.SEVERE, "CreateOFCDetail", ex);
			FDialog.error(m_WindowNo, this, ex.getLocalizedMessage());
			m_initOK = false;
		}
		//
		this.setHeight("500px");
		this.setWidth("700px");
		this.setBorder("normal");
	} // VPayment

	private void zkInit() throws Exception {
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
		eLabel.setText(Msg.translate(Env.getCtx(), "DocumentNo"));
		North north = new North();
		north.setStyle("border: none");
		mainLayout.appendChild(north);
		north.appendChild(northPanel);
		northPanel.appendChild(eLabel);
		northPanel.appendChild(new Space());
		northPanel.appendChild(eText);
		eText.setCols(2);
		eText.setWidth("20%");
		//
		South south = new South();
		south.setStyle("border: none");
		mainLayout.appendChild(south);
		south.appendChild(confirmPanel);
		confirmPanel.addActionListener(this);
	} // jbInit

	private boolean dynInit(WButtonEditor button) throws Exception {
		miniTable(null);
		return true;
	} // dynInit

	private void miniTable(String DocumentNo) {

		Vector<Vector> data = getShipment(m_mTab.getRecord_ID(), DocumentNo);
		ColumnInfo[] layout = new ColumnInfo[] {
				new ColumnInfo(" ", ".", IDColumn.class, false, false, ""),
				new ColumnInfo(
						Msg.translate(Env.getCtx(), "ShipmentDocumentNo"), ".",
						String.class),
				new ColumnInfo(Msg.translate(Env.getCtx(),
						"M_WarehouseSource_Name"), ".", String.class),
				new ColumnInfo(Msg.translate(Env.getCtx(), "M_Warehouse_Name"),
						".", String.class),
				new ColumnInfo(Msg.translate(Env.getCtx(), "TransportAgency"),
						".", String.class), };
		xTable.prepareTable(layout, "", "", false, "");
		xTable.setMultiSelection(true);
		xTable.loadTable(data);
	}

	// 查询出的物流单满足（1.没有与其他【OFC收货单】绑定的物流单。 2.与本身【OFC收货单】绑定的物流单,3.单据类型）
	private Vector getShipment(int OFC_ID, String DocumentNo) {
		// select *
		// from wt_owms_shipment
		// where ( wt_owms_shipment.wt_ofc_id is null
		// or wt_owms_shipment.wt_ofc_id = ? ) and
		// wt_owms_shipment.m_warehouse_id =?

		if (StringUtils.isNotBlank(DocumentNo)) {
			DocumentNo = " ) AND  wt_owms_shipment.documentno = '" + DocumentNo
					+ "'" + " AND wt_owms_shipment.m_warehouse_id =?";
		} else {
			DocumentNo = " ) AND  wt_owms_shipment.m_warehouse_id =? ";
		}
		if (StringUtils.isNotBlank(currentReceiptType)) {
			DocumentNo += " AND wt_owms_shipment.receipttype ='" + currentReceiptType + "' ";
		}
		final String whereClause = " ( " + X_WT_OWMS_Shipment.Table_Name + "."
				+ X_WT_OWMS_Shipment.COLUMNNAME_WT_OFC_ID + " is null OR "
				+ X_WT_OWMS_Shipment.Table_Name + "."
				+ X_WT_OWMS_Shipment.COLUMNNAME_WT_OFC_ID + " = ? "
				+ DocumentNo;
		//增加按照DocumentNo排序功能，BUG 6522
		List<X_WT_OWMS_Shipment> shipments = new Query(Env.getCtx(),
				X_WT_OWMS_Shipment.Table_Name, whereClause, null)
				.setParameters(OFC_ID,
						Env.getContext(Env.getCtx(), "#M_Warehouse_ID")).setOrderBy("CREATED").list();
 
		Vector data = new Vector();
		if (!shipments.isEmpty()) {
			for (X_WT_OWMS_Shipment shipment : shipments) {
				Vector row = new Vector();
				row.add(shipment.getWT_OWMS_Shipment_ID());
				row.add(shipment.getDocumentNo() == null ? "" : shipment
						.getDocumentNo());
				row.add(shipment.getM_WarehouseSource_Name() == null ? ""
						: shipment.getM_WarehouseSource_Name());
				row.add(shipment.getM_Warehouse().getName() == null ? ""
						: shipment.getM_Warehouse().getName());
				row.add(shipment.getC_BPartner().getName() == null ? ""
						: shipment.getC_BPartner().getName());
				data.add(row);
			}
		}
		return data;
	} // saveChanges

	@Override
	public void onEvent(Event e) {
		// Finish

		if (e.getTarget().getId().equals(ConfirmPanel.A_REFRESH)) {
			String DocumentNo = null == eText.getText() ? "" : eText.getText();
			miniTable(DocumentNo);
		}

		if (e.getTarget().getId().equals(ConfirmPanel.A_OK)) {
			saveChanges(); // cannot recover
			dispose();
		} else if (e.getTarget().getId().equals(ConfirmPanel.A_CANCEL)) {
			dispose();
		}
	}

	private void saveChanges() {
		ArrayList<Integer> results = new ArrayList<Integer>();
		int rows = xTable.getRowCount();
		for (int i = 0; i < rows; i++) {
			IDColumn id = (IDColumn) xTable.getValueAt(i, 0); // ID in column 0
			// log.fine( "Row=" + i + " - " + id);
			if (id != null && id.isSelected())
				results.add(id.getRecord_ID());
		}

		if (results != null && results.size() > 0) {
			// 创建一个事务
			String trxName = Trx.createTrxName("pInfo");
			try {
				MOFC ofc = new MOFC(Env.getCtx(), m_mTab.getRecord_ID(),
						trxName);
				// 清空此【OFC】单与【单品】和【包裹】,【跨国物流单】的关联
				ofc.cleanItemAndPackage();

				// 清空此【OFC】单与【收货明细】和【收货单据】的关联
				ofc.cleanOFCDeatilAndRecepit();

				for (int shipment_ID : results) {
					// 更新OFC收货单的信息
					setInfoByShipments(shipment_ID, m_mTab.getRecord_ID(),
							trxName);
				}
				// 对所有单品的产品ID做distinct再反写到收货明细中
				linkOfcDetail(m_mTab.getRecord_ID(), trxName);

				// 算出所有包裹数再反写到OFC收货单中
				ofc.setPackageNum(ofc.countPackageQty());
				// 将生成明细的时间写入收货日期
				ofc.setDateReceive(new Timestamp(System.currentTimeMillis()));

				ofc.saveEx();
				Trx.get(trxName, false).commit();
			} catch (Exception exception) {
				Trx.get(trxName, false).rollback();
			}
		}
	}

	public void setInfoByShipments(int WT_Shipment_id, int OFC_ID,
			String trxName) {
		// 将【OFC】单与选中的【物流单】中的【单品】和 【包裹】关联
		LinkItemAndPackage(WT_Shipment_id, OFC_ID, trxName);

		// 更新收货单据
		updateReceipt(WT_Shipment_id, OFC_ID, trxName);

		// 更新跨国物流单
		X_WT_OWMS_Shipment shipment = new X_WT_OWMS_Shipment(Env.getCtx(),
				WT_Shipment_id, trxName);
		shipment.setWT_OFC_ID(OFC_ID);
		shipment.saveEx();
	}

	/**
	 * 将【OFC】单与选中的【物流单】中的【单品】 和 【包裹】关联
	 * 
	 * @param WT_Shipment_id
	 * @param OFC_ID
	 * @param trxName
	 */
	private void LinkItemAndPackage(int WT_Shipment_id, int OFC_ID,
			String trxName) {
		String sql = "UPDATE WT_ItemInfo i set i.wt_ofc_id = " + OFC_ID
				+ " , i.dateofc=sysdate Where i.wt_owms_shipment_id  ="
				+ WT_Shipment_id;
		int no = DB.executeUpdate(sql.toString(), trxName);
		if (no != 0) {
			log.log(Level.SEVERE, sql.toString());
		}

		String sql2 = "UPDATE WT_PackageInfo i set i.wt_ofc_id = " + OFC_ID
				+ " , i.dateofc=sysdate Where i.wt_owms_shipment_id  ="
				+ WT_Shipment_id;
		int no2 = DB.executeUpdate(sql2.toString(), trxName);
		if (no2 != 0) {
			log.log(Level.SEVERE, sql2.toString());
		}
	}

	/**
	 * 对所有单品的产品ID做distinct再反写到收货明细中
	 * 
	 * @param OFC_ID
	 * @param trxName
	 */
	public void linkOfcDetail(int OFC_ID, String trxName) {
		String sql = " SELECT DISTINCT i.m_product_id, count(1) as qty from wt_iteminfo i where i.wt_ofc_id = "
				+ OFC_ID + " GROUP BY i.m_product_id ";
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = DB.prepareStatement(sql.toString(), trxName);
			rs = pstmt.executeQuery();
			while (rs.next()) {
				MOFCDetail detail = new MOFCDetail(Env.getCtx(), 0, trxName);
				detail.setM_Product_ID(rs.getInt("m_product_id"));
				detail.setWT_OFC_ID(OFC_ID);
				detail.setQty(rs.getInt("qty"));
				detail.saveEx();
			}
		} catch (SQLException e) {
			throw new AdempiereException(sql.toString());
		} finally {
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
		}
	}

	private void updateReceipt(int WT_Shipment_id, int OFC_ID, String trxName) {
		X_WT_OWMS_Shipment shipment = new X_WT_OWMS_Shipment(Env.getCtx(),
				WT_Shipment_id, trxName);
		X_WT_Receipt recepit = new X_WT_Receipt(Env.getCtx(), 0, trxName);
		recepit.setDocumentNo(shipment.getDocumentNo());
		recepit.setM_WarehouseSource_Name(shipment.getM_WarehouseSource_Name());
		recepit.setDestination(shipment.getM_Warehouse_ID());
		recepit.setWT_OFC_ID(OFC_ID);
		recepit.setWT_OWMS_Shipment_ID(WT_Shipment_id);
		recepit.saveEx();
	}

	public boolean isInitOK() {
		return m_initOK;
	} // isInitOK

	@Override
	public boolean needSave() {
		return false;
	}

	@Override
	public String getErrorMessage() {
		return null;
	}

}
