package jp.co.infitech.astah;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import com.change_vision.jude.api.inf.AstahAPI;
import com.change_vision.jude.api.inf.model.IDiagram;
import com.change_vision.jude.api.inf.model.IModel;
import com.change_vision.jude.api.inf.model.INamedElement;
import com.change_vision.jude.api.inf.presentation.IPresentation;
import com.change_vision.jude.api.inf.project.ProjectAccessor;
import com.change_vision.jude.api.inf.project.ProjectEvent;
import com.change_vision.jude.api.inf.project.ProjectEventListener;
import com.change_vision.jude.api.inf.ui.IPluginExtraTabView;
import com.change_vision.jude.api.inf.ui.ISelectionListener;
import com.change_vision.jude.api.inf.view.IDiagramViewManager;
import com.change_vision.jude.api.inf.view.IViewManager;

public class ToDoView extends JPanel implements IPluginExtraTabView, ProjectEventListener {
    private ProjectAccessor projectAccessor;
    private IModel project = null;
    private IViewManager ivm = null;

    private String[] columnTitle = {"TODO", "Diagram Name", "Namespace", "Object"};

    /** TODOリスト */
    private JTable table;


    public ToDoView() {
        setLayout(new BorderLayout());
        table = new JTable();
        table.setAutoCreateRowSorter(true);
        JScrollPane scrollPane = new JScrollPane(table);
        add(scrollPane, BorderLayout.CENTER);
        addProjectEventListener();
          getTodo();
    }

    @Override
    public void projectChanged(ProjectEvent arg0) {
        System.out.println("projectChanged()");
           getTodo();
    }

    @Override
    public void projectClosed(ProjectEvent arg0) {
    }

    @Override
    public void projectOpened(ProjectEvent arg0) {
        System.out.println("projectOpened()");
          getTodo();
    }

    @Override
    public void activated() {
    }

    @Override
    public void addSelectionListener(ISelectionListener arg0) {
    }

    @Override
    public void deactivated() {
    }

    @Override
    public Component getComponent() {
        return this;
    }

    @Override
    public String getDescription() {
        return "Task List";
    }

    @Override
    public String getTitle() {
        return "Task";
    }

    private void addProjectEventListener() {
        try {
            AstahAPI api = AstahAPI.getAstahAPI();
            projectAccessor = api.getProjectAccessor();
            projectAccessor.addProjectEventListener(this);
            ivm = projectAccessor.getViewManager();
        }
        catch (Exception e) {
            e.getMessage();
        }
    }

    private void getTodo() {
        try {
            ArrayList<Note> notes = new ArrayList<Note>();
            project = projectAccessor.getProject();
            INamedElement[] seqdiagrams = projectAccessor.findElements(IDiagram.class);
            for(INamedElement seqdiagram : seqdiagrams) {
                IPresentation[] iPresentations = seqdiagram.getPresentations();
                for(IPresentation iPresentation : iPresentations) {
                    if(iPresentation.getType().equals("Note") == true) {
                        Pattern pattern = Pattern.compile("^((?i)TODO).*");
                        Matcher matcher = pattern.matcher(iPresentation.getLabel());
                        if(matcher.find() == true) {
                            Note note = new Note();
                            note.namespace = seqdiagram.getFullNamespace("/");
                            note.name = seqdiagram.getName();
                            note.note = iPresentation.getLabel().replaceFirst("^((?i)TODO)", "");
                            note.diagram = seqdiagram;
                            notes.add(note);
                        }
                    }
                }
            }
            Collections.sort(notes, new Comparator<Note>() {
                public int compare(Note o1, Note o2) {
                    int result = o1.namespace.compareTo(o2.namespace);
                    if(result == 0) {
                        result = o1.name.compareTo(o2.name);
                    }
                    return result;
                }
            });

            Object[][] lalala = new Object[notes.size()][4];
            for(int i = 0; i< notes.size(); i++) {
                lalala[i][0] = notes.get(i).note;
                lalala[i][1] = notes.get(i).name;
                lalala[i][2] = notes.get(i).namespace;
                lalala[i][3] = notes.get(i).diagram;
            }
            final DefaultTableModel todolist = new DefaultTableModel(lalala, columnTitle) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };
            table.setModel(todolist);
            table.getColumnModel().getColumn(3).setPreferredWidth(0);
            table.getColumnModel().getColumn(3).setMinWidth(0);
            table.getColumnModel().getColumn(3).setMaxWidth(0);
//        	int width = (int)table.getSize().width;
//        	System.out.println("Width : " + table.getSize().width);
//        	table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
//        	table.getColumnModel().getColumn(0).setPreferredWidth((int)(width * 0.5));

            table.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent me) {
                    if(me.getClickCount() == 2) {
                        Point point = me.getPoint();
                        int index = table.rowAtPoint(point);
                        if(index >= 0) {
                            int row = table.convertRowIndexToModel(index);
                            IDiagramViewManager idvm = ivm.getDiagramViewManager();
                            idvm.open((IDiagram)todolist.getValueAt(row, 3));
                        }
                    }
                }
            });
        }
        catch (Exception e) {
            JOptionPane.showMessageDialog(this.getParent(), e.toString(), "Alert", JOptionPane.ERROR_MESSAGE);
        }
    }
}
