// Administrador de 
package gt.edu.umg.sistemas;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class TaskManagerSearchGUI extends JFrame {

    private JTextField txtInputApp;
    private JTextField txtBuscar;
    private JTable tablaProcesos;
    private DefaultTableModel modeloTabla;
    private TableRowSorter sorter;
    private JButton btnListar;

    public TaskManagerSearchGUI() {
        setTitle("Administrador de procesos");
        setSize(750, 550);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        JPanel panelSuperior = new JPanel(new GridLayout(2, 1));

        JPanel panelControles = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        txtInputApp = new JTextField(12);
        JButton btnAbrir = new JButton("Abrir App");
        JButton btnCerrar = new JButton("Cerrar Seleccionado/Escrito");
        btnListar = new JButton("Actualizar Procesos");

        panelControles.add(new JLabel("Ejecutable/PID:"));
        panelControles.add(txtInputApp);
        panelControles.add(btnAbrir);
        panelControles.add(btnCerrar);
        panelControles.add(btnListar);

        JPanel panelBusqueda = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        txtBuscar = new JTextField(25);
        panelBusqueda.add(new JLabel("🔍 Buscar proceso:"));
        panelBusqueda.add(txtBuscar);

        panelSuperior.add(panelControles);
        panelSuperior.add(panelBusqueda);

        String[] columnas = {"Nombre de Imagen", "PID", "Nombre de Sesión", "Núm. Sesión", "Uso de Memoria"};
        modeloTabla = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tablaProcesos = new JTable(modeloTabla);
        tablaProcesos.getTableHeader().setReorderingAllowed(false);
        tablaProcesos.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); 
        
        sorter = new TableRowSorter<>(modeloTabla);
        tablaProcesos.setRowSorter(sorter);

        JScrollPane scrollPane = new JScrollPane(tablaProcesos);

        add(panelSuperior, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);

        JPopupMenu menuContextual = new JPopupMenu();
        JMenuItem itemCerrar = new JMenuItem("Finalizar Proceso");
        menuContextual.add(itemCerrar);

        itemCerrar.addActionListener(e -> cerrarProcesoSeleccionado());

        tablaProcesos.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    int filaVista = tablaProcesos.rowAtPoint(e.getPoint());
                    if (filaVista >= 0 && filaVista < tablaProcesos.getRowCount()) {
                        tablaProcesos.setRowSelectionInterval(filaVista, filaVista);
                        menuContextual.show(e.getComponent(), e.getX(), e.getY());
                    }
                }
            }
        });

        
    private void filtrarTabla() {
        String textoBusqueda = txtBuscar.getText();
        if (textoBusqueda.trim().length() == 0) {
            sorter.setRowFilter(null);
        } else {
            sorter.setRowFilter(RowFilter.regexFilter("(?i)" + textoBusqueda));
        }
    }

    private void cerrarProcesoSeleccionado() {
        int filaVista = tablaProcesos.getSelectedRow();
        if (filaVista != -1) {
            int filaModelo = tablaProcesos.convertRowIndexToModel(filaVista);
            
            String pid = (String) modeloTabla.getValueAt(filaModelo, 1);
            String nombre = (String) modeloTabla.getValueAt(filaModelo, 0);
            
            int confirmacion = JOptionPane.showConfirmDialog(
                    this, 
                    "¿Estás seguro de que deseas cerrar '" + nombre + "' (PID: " + pid + ")?", 
                    "Confirmar cierre", 
                    JOptionPane.YES_NO_OPTION);
                    
            if (confirmacion == JOptionPane.YES_OPTION) {
                cerrarApp(pid);
            }
        } else {
            JOptionPane.showMessageDialog(this, "Por favor, selecciona un proceso en la tabla primero.");
        }
    }

    private void abrirApp(String nombreApp) {
        if (nombreApp.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Ingresa el nombre de la aplicación.");
            return;
        }
        try {
            Runtime.getRuntime().exec(nombreApp);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error al abrir: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void cerrarApp(String procesoOPid) {
        try {
            String flag = procesoOPid.matches("\\d+") ? "/PID" : "/IM";
            String comando = "taskkill /F " + flag + " " + procesoOPid;
            
            Runtime.getRuntime().exec(comando);
            
            Timer timer = new Timer(1000, evt -> listarProcesos());
            timer.setRepeats(false);
            timer.start();

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error al cerrar: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
  private void listarProcesos() {
        btnListar.setEnabled(false);
        btnListar.setText("Cargando...");

        new Thread(() -> {
            try {
                Process proceso = Runtime.getRuntime().exec("tasklist /FO CSV /NH");
                BufferedReader lector = new BufferedReader(new InputStreamReader(proceso.getInputStream()));
                String linea;
                
                Object[][] nuevosDatos = new Object[1000][5];
                int filasLeidas = 0;

                while ((linea = lector.readLine()) != null) {
                    if (linea.startsWith("\"") && linea.endsWith("\"")) {
                        linea = linea.substring(1, linea.length() - 1);
                    }
                    
                    String[] datosFila = linea.split("\",\"");
                    
                    if(datosFila.length == 5 && filasLeidas < nuevosDatos.length) {
                        nuevosDatos[filasLeidas] = datosFila;
                        filasLeidas++;
                    }
                }

                final int totalFilas = filasLeidas;

                SwingUtilities.invokeLater(() -> {
                    modeloTabla.setRowCount(0);
                    for (int i = 0; i < totalFilas; i++) {
                        modeloTabla.addRow(nuevosDatos[i]);
                    }
                    btnListar.setEnabled(true);
                    btnListar.setText("Actualizar Procesos");
                    
                    filtrarTabla();
                });

            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this, "Fallo al listar: " + ex.getMessage());
                    btnListar.setEnabled(true);
                    btnListar.setText("Actualizar Procesos");
                });
            }
        }).start();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new TaskManagerSearchGUI().setVisible(true);
        });
    }
}
