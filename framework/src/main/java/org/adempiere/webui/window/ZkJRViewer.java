package org.adempiere.webui.window;

import java.io.File;
import java.util.logging.Level;

import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperPrint;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.webui.component.Window;
import org.compiere.util.CLogger;
import org.zkoss.util.media.AMedia;
import org.zkoss.zkex.zul.Borderlayout;
import org.zkoss.zkex.zul.Center;
import org.zkoss.zkex.zul.North;
import org.zkoss.zul.Iframe;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;
import net.sf.jasperreports.engine.export.JRXlsExporter;
import net.sf.jasperreports.engine.export.JRXlsExporterParameter;
import org.adempiere.webui.component.Listbox;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.Separator;
import java.io.FileOutputStream;

public class ZkJRViewer extends Window implements EventListener
{

	private static final long	serialVersionUID	= 2021796699437770927L;

	private JasperPrint			jasperPrint;
	private Listbox				previewType			= new Listbox();
	private Iframe				iframe				= null;
	private AMedia				media				= null;

	/** Logger */
	private static CLogger		log					= CLogger.getCLogger(ZkJRViewer.class);

	public ZkJRViewer(JasperPrint jasperPrint, String title)
	{
		super();
		this.setTitle(title);
		this.jasperPrint = jasperPrint;
		init();
	}

	private void init()
	{
		Borderlayout layout = new Borderlayout();
		layout.setStyle("position: absolute; height: 99%; width: 99%");
		this.appendChild(layout);
		this.setStyle("width: 100%; height: 100%; position: absolute");

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("26px");
		Toolbarbutton button = new Toolbarbutton();
		button.setImage("/images/Print24.png");
		button.setTooltiptext("Print");
		toolbar.appendChild(button);

		North north = new North();
		layout.appendChild(north);
		north.appendChild(toolbar);

		Center center = new Center();
		center.setFlex(true);
		layout.appendChild(center);
		iframe = new Iframe(); 
		iframe.setId("reportFrame");
		iframe.setHeight("100%");
		iframe.setWidth("100%");

		toolbar.appendChild(new Separator("vertical"));

		previewType.setMold("select");

		previewType.appendItem("PDF", "PDF");
		previewType.appendItem("Excel", "XLS");
		toolbar.appendChild(previewType);
		previewType.addEventListener(Events.ON_SELECT, this);

		try
		{
			renderReport();
		}
		catch (Exception e)
		{
			log.log(Level.SEVERE, e.getLocalizedMessage(), e);
			throw new AdempiereException("Failed to render report.", e);
		}
		center.appendChild(iframe);

		this.setBorder("normal");
	}

	private void renderReport() throws Exception
	{
		Listitem selected = previewType.getSelectedItem();
		if (selected == null || "PDF".equals(selected.getValue()))
		{
			String path = System.getProperty("java.io.tmpdir");
			String prefix = makePrefix(jasperPrint.getName());
			if (log.isLoggable(Level.FINE))
			{
				log.log(Level.FINE, "Path=" + path + " Prefix=" + prefix);
			}
			File file = File.createTempFile(prefix, ".pdf", new File(path));
			JasperExportManager.exportReportToPdfFile(jasperPrint, file.getAbsolutePath());
			media = new AMedia(getTitle(), "pdf", "application/pdf", file, true);
		}
		else if ("XLS".equals(previewType.getSelectedItem().getValue()))
		{
			String path = System.getProperty("java.io.tmpdir");
			String prefix = makePrefix(jasperPrint.getName());
			if (log.isLoggable(Level.FINE))
			{
				log.log(Level.FINE, "Path=" + path + " Prefix=" + prefix);
			}
			File file = File.createTempFile(prefix, ".xls", new File(path));
			FileOutputStream fos = new FileOutputStream(file);
			JRXlsExporter exporterXLS = new JRXlsExporter();
			exporterXLS.setParameter(JRXlsExporterParameter.JASPER_PRINT, jasperPrint);
			exporterXLS.setParameter(JRXlsExporterParameter.OUTPUT_STREAM, fos);
			exporterXLS.setParameter(JRXlsExporterParameter.OUTPUT_FILE, file.getAbsolutePath());
			exporterXLS.setParameter(JRXlsExporterParameter.IS_DETECT_CELL_TYPE, Boolean.TRUE);
			exporterXLS.setParameter(JRXlsExporterParameter.IS_WHITE_PAGE_BACKGROUND, Boolean.FALSE);
			exporterXLS.setParameter(JRXlsExporterParameter.IS_REMOVE_EMPTY_SPACE_BETWEEN_ROWS, Boolean.TRUE);
			exporterXLS.setParameter(JRXlsExporterParameter.IS_REMOVE_EMPTY_SPACE_BETWEEN_COLUMNS, Boolean.TRUE);
			exporterXLS.setParameter(JRXlsExporterParameter.IS_COLLAPSE_ROW_SPAN, Boolean.TRUE);
			exporterXLS.setParameter(JRXlsExporterParameter.IS_IGNORE_GRAPHICS, Boolean.FALSE);
			exporterXLS.exportReport();
			media = new AMedia(getTitle(), "xls", "application/vnd.ms-excel", file, true);
		}
		iframe.setContent(media);
	}

	private String makePrefix(String name)
	{
		StringBuffer prefix = new StringBuffer();
		char[] nameArray = name.toCharArray();
		for (char ch : nameArray)
		{
			if (Character.isLetterOrDigit(ch))
			{
				prefix.append(ch);
			}
			else
			{
				prefix.append("_");
			}
		}
		return prefix.toString();
	}

	public void onEvent(Event event) throws Exception
	{
		if (event.getName().equals(Events.ON_CLICK) || event.getName().equals(Events.ON_SELECT))
			renderReport();
	}
}
