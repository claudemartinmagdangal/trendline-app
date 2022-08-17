package jclass.chart;

import jclass.dto.StockTrendInfo;
import jclass.dto.StockTrendInfoTableModel;
import jclass.dto.ToolTipHeader;
import jclass.enums.CandleChartsEnum;
import jclass.io.FileUtils;
import jclass.io.StockFileHandling;
import jclass.util.OHLCCandleStickTrendUtil;
import jclass.util.ReadExcel;
import jclass.util.Utils;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.*;
import java.util.stream.Stream;

public class StockTable {

    // property for use for stock

    private List<StockTrendInfo> stockInfo;
    static JProgressBar b;
    private JFrame f, jFrame;
    private int totalDownloadedFileCount = 0;
    private TableModel tableModel;
    private JTable table;
    private JScrollPane sp, spHead;
    private StockFileHandling sfh;
    private Box box;
    private JPanel tablePanel, mainPanel;
    //private StockTrendInfoTableModel stockTrendInfoTableModel;

    private JTextField symbleJF;
    private JTextField mPC_UpwardTrendJF;
    private JTextField mPC_DownWardTrendJF;
    private JTextField m_U_TrendCountJF;
    private JTextField m_D_TrendCountJF;
    private JTextField t_U_T_DistanceJF;
    private JTextField t_D_T_DistanceJF;
    private JTextField volumeJF;

    private String[] VOLUMOPERATOR = {"==", ">", "<", ">=", "<=", "!="};
    private String[] MPCUTOPERATOR = {"==", ">", "<", ">=", "<=", "!="};
    private String[] MPCDTOPERATOR = {"==", ">", "<", ">=", "<=", "!="};
    private String[] MUTCOPERATOR = {"==", ">", "<", ">=", "<=", "!="};
    private String[] MDTCOPERATOR = {"==", ">", "<", ">=", "<=", "!="};
    private String[] TUTOPERATOR = {"==", ">", "<", ">=", "<=", "!="};
    private String[] TDTOPERATOR = {"==", ">", "<", ">=", "<=", "!="};

    private JComboBox volumComboList;
    private JComboBox mpcutComboList;
    private JComboBox mpcdtComboList;
    private JComboBox mutcComboList;
    private JComboBox mdtcComboList;
    private JComboBox tutComboList;
    private JComboBox tdtComboList;

    public StockTable() throws UnsupportedLookAndFeelException, ClassNotFoundException, InstantiationException, IllegalAccessException {
        JFrame f = new JFrame();
        getDataList();
        sfh = new StockFileHandling();

        tablePanel = new JPanel();
        tablePanel.setLayout(new BorderLayout(0, 1));
        tableModel = new StockTrendInfoTableModel(stockInfo);
        table = new JTable(tableModel);
        ///table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        table = tableAdjest(table);

        // Tooltips
        ToolTipHeader tooltipHeader = new ToolTipHeader(table.getColumnModel());
        tooltipHeader.setToolTipStrings(StockTrendInfoTableModel.getColumnNames());
        table.setTableHeader(tooltipHeader);


        //Industry List

        ArrayList<JCheckBox> cb = new ArrayList<>();
        box = Box.createVerticalBox();
        JButton searchHeadItems = new JButton("Search Items");
        JButton clearHeadItems = new JButton("Clear ");

        JPanel pan = new JPanel();
        pan.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();

        List<String> hlist = ReadExcel.headList();
        JCheckBox checkSelectAll = new JCheckBox("Select All");
        cb.add(checkSelectAll);
        box.add(checkSelectAll);

        for (int i = 0; i < hlist.size(); i++) {
            JCheckBox checkBox = new JCheckBox(hlist.get(i));
            cb.add(checkBox);
            box.add(checkBox);

        }

        spHead = new JScrollPane(box);
        spHead.setPreferredSize(new Dimension(180, 620));
        c.fill = GridBagConstraints.PAGE_START;
        c.weightx = 0.5;
        c.gridx = 1;
        c.gridy = 0;
        pan.add(spHead, c);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.ipady = 0;       //reset to default
        c.weighty = 1.0;   //request any extra vertical space
        c.anchor = GridBagConstraints.PAGE_END; //bottom of space
        c.insets = new Insets(10, 0, 0, 0);  //top padding
        c.gridx = 1;       //aligned with button 2
        c.gridwidth = 2;   //2 columns wide
        c.gridy = 2;       //third row
        pan.add(searchHeadItems, c);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.ipady = 0;       //reset to default
        c.weighty = 1.0;   //request any extra vertical space
        c.anchor = GridBagConstraints.PAGE_END; //bottom of space
        c.insets = new Insets(10, 0, 0, 0);  //top padding
        c.gridx = 2;       //aligned with button 2
        c.gridwidth = 2;   //2 columns wide
        c.gridy = 2;       //third row
        pan.add(clearHeadItems, c);
        // pan.add(clearHeadItems);

        searchHeadItems.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                List<StockTrendInfo> search = new ArrayList<>();
                for (JCheckBox c : cb) {
                    if (c.isSelected()) {
                        for (StockTrendInfo sf : stockInfo) {
                            if (sf.getIndustry().equals(c.getText())) {
                                search.add(sf);
                            }
                        }
                        // System.out.println(c.getText());
                    }
                }

                //update table
                //  sp=new JScrollPane(table,JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
                tableModel = new StockTrendInfoTableModel(search);
                table.setModel(tableModel);
                // tablePanel.removeAll();
                // tablePanel.add(sp, BorderLayout.CENTER);
                tablePanel.updateUI();
                sp.updateUI();
                box.updateUI();
                table = tableAdjest(table);
            }
        });

        clearHeadItems.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                for (JCheckBox c : cb) {
                    c.setSelected(false);
                }
            }
        });

        checkSelectAll.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                for (JCheckBox c : cb) {
                    if (checkSelectAll.isSelected())
                        c.setSelected(true);
                    else
                        c.setSelected(false);
                }
            }
        });

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout());
        JButton downloadStock = new JButton("Download Stocks");
        downloadStock.setToolTipText("Removing all the stock file download newly ");
        JButton reloadData = new JButton("Reload Data");
        reloadData.setToolTipText("Reload all the data from the files");
        JButton refreshTable = new JButton("Refresh Table");
        refreshTable.setToolTipText("Refresh the data");
        buttonPanel.add(downloadStock);
        buttonPanel.add(reloadData);
        buttonPanel.add(refreshTable);
        JPanel card1 = new JPanel();
        card1.setLayout(new FlowLayout());


        JPanel searhPanel = new JPanel();
        GridBagLayout layout = new GridBagLayout();
        searhPanel.setLayout(layout);
        GridBagConstraints gbc = new GridBagConstraints();

        searhPanel.setLayout(new GridLayout(2, 2));
        JButton searchbtn = new JButton("Search");
        searchbtn.setToolTipText("Filter Data of the table");

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridwidth = 2;
        searhPanel.add(searchbtn, gbc);

        card1.add(buttonPanel);

        JPanel p = new JPanel();
        b = new JProgressBar();
        b.setValue(0);
        b.setStringPainted(true);
        b.setVisible(false);
        p.add(b);

        downloadStock.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                deleteAllfiles();
                b.setVisible(true);

                Thread thread = new Thread() {
                    public void run() {
                        sfh.downloadStocksHistoryIntoFiles(Utils.STOCK_HISTORY_OF_DAY);
                    }
                };
                thread.start();

                Thread thread1 = new Thread() {
                    public void run() {
                        fileDownloadStatus();
                    }
                };
                thread1.start();
            }
        });

        reloadData.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                updateTable();
            }
        });

        refreshTable.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                refreshTable();
            }
        });


        JPanel card3 = new JPanel(new GridLayout(15, 1, 10, 5));
        JButton showGraphbnt = new JButton("Show Trend-Line");
        showGraphbnt.setToolTipText("Show the Trend-Line Graph on selected row");
        JCheckBox jboxLogChart = new JCheckBox("Log Chart", false);
        JButton filterBtn = new JButton("Filter");
        filterBtn.setToolTipText("Filter Data of the table");
        //JButton ShowTrendBtn = new JButton("Show trending");
        //ShowTrendBtn.setToolTipText("Show trending in the market");

        showGraphbnt.setEnabled(false);
        card3.add(showGraphbnt);
        card3.add(jboxLogChart);
        card3.add(filterBtn);
        //card3.add(ShowTrendBtn);

        showGraphbnt.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Boolean isSelected = jboxLogChart.isSelected();
                showChart(isSelected);
            }
        });

        filterBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                searchingPanel();
            }
        });

        /*ShowTrendBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                System.out.println("show trend button has been clicked!");
            }
        });*/


        ListSelectionModel selectionModel = table.getSelectionModel();

        selectionModel.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                showGraphbnt.setEnabled(true);
            }
        });


        searchbtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                searchingPanel();
            }
        });


        // Table column moved listener To-Task
        table.setAutoCreateRowSorter(true);
        TableRowSorter<TableModel> sorter = new TableRowSorter<>(table.getModel());
        table.setRowSorter(sorter);
        table.setBounds(30, 40, 900, 800);
        table.setFillsViewportHeight(true);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setPreferredScrollableViewportSize(table.getPreferredSize());

        table.getTableHeader().addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
            }

            @Override
            public void mousePressed(MouseEvent e) {
            }

            @Override
            public void mouseReleased(MouseEvent e) {

                List<String> list = new LinkedList<String>();
                for (int i = 0; i < table.getColumnCount(); ++i) {
                    list.add(table.getColumnName(i));
                }
//                System.out.println(list);
                FileUtils.writeFile(list);
            }

            @Override
            public void mouseEntered(MouseEvent e) {

            }

            @Override
            public void mouseExited(MouseEvent e) {

            }
        });

        sp = new JScrollPane(table, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        tablePanel.add(sp, BorderLayout.CENTER);
        tablePanel.add(pan, BorderLayout.WEST);

        f.add(card1, BorderLayout.PAGE_START);
        f.add(tablePanel, BorderLayout.CENTER);
        f.add(card3, BorderLayout.LINE_END);
        f.add(p, BorderLayout.PAGE_END);

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        double width = screenSize.getWidth();
        double height = screenSize.getHeight();

        f.setSize((int) width, (int) height);
        f.setVisible(true);
        tablePanel.updateUI();
        sp.updateUI();
        box.updateUI();
        UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
        //UIManager.setLookAndFeel("com.sun.java.swing.plaf.motif.MotifLookAndFeel");

    }

    public void getDataList() {
        stockInfo = OHLCCandleStickTrendUtil.calculateTrendsInfo(true);
        //stockInfo.sort(new StockTrendInfo.SortByMaxUpwardTrendCount());
    }

    public void updateTable() {
        getDataList();
        tableModel = new StockTrendInfoTableModel(stockInfo);
        table.setModel(tableModel);
        table.updateUI();
        sp.updateUI();

        tablePanel.updateUI();
        sp.updateUI();
        box.updateUI();

        table = tableAdjest(table);
        b.setVisible(false);
    }

    public void refreshTable() {
        tableModel = new StockTrendInfoTableModel(stockInfo);
        table.setModel(tableModel);
        table.updateUI();
        table = tableAdjest(table);
        sp.updateUI();
        b.setVisible(false);

        tablePanel.updateUI();
        sp.updateUI();
        box.updateUI();
    }

    public void fileDownloadStatus() {
        Integer total = sfh.getTotalStock();
        Integer count = 0;
        try {

            int repeat = 0;
            int previous = 0;
            while (count <= total) {
                count = fileDowloadedStatus() + sfh.getDownloadFailStock();
                b.setString("Total : " + count + "/" + total + " Fail " + sfh.getDownloadFailStock());
                b.setValue((count * 100) / total);
                Thread.sleep(500);
                count = count + 1;

                if (previous != count) {
                    previous = count;
                    repeat = 0;
                } else {
                    repeat++;
                }

                if (repeat == 30) {
                    break;
                }
            }
            b.setString("Tom, Please Wait...data is loading into the table..");
            updateTable();
        } catch (Exception e) {
        }
    }

    public int fileDowloadedStatus() throws IOException {
        try (Stream<Path> files = Files.list(Paths.get("downloads\\"))) {
            totalDownloadedFileCount = (int) files.count();
        }
        //System.out.println("download : " + totalDownloadedFileCount);
        return totalDownloadedFileCount;
    }

    private void deleteAllfiles() {
        File dir = new File(String.valueOf(Paths.get("downloads\\")));

        if (dir.exists()) {
            if (dir.listFiles().length != 0) {
                for (File file : dir.listFiles()) {
                    if (!file.isDirectory()) {
                        file.delete();
                    }
                }
                //System.out.println("All Files have Deleted!");
            }
        } else {
            //System.out.println("Could not find the download Folder, please create it!!!");
            JOptionPane.showMessageDialog(f, "Could not find the download Folder, please create it!!!", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void searchingPanel() {

        JFrame frame = new JFrame("Searching Panel");
        frame.setAlwaysOnTop(true);

        JPanel panel = new JPanel();
        GridBagLayout layout = new GridBagLayout();

        panel.setLayout(layout);
        GridBagConstraints gbc = new GridBagConstraints();

        mpcutComboList = new JComboBox(MPCUTOPERATOR);
        mpcdtComboList = new JComboBox(MPCDTOPERATOR);
        mutcComboList = new JComboBox(MUTCOPERATOR);
        mdtcComboList = new JComboBox(MDTCOPERATOR);
        tutComboList = new JComboBox(TUTOPERATOR);
        tdtComboList = new JComboBox(TDTOPERATOR);
        volumComboList = new JComboBox(VOLUMOPERATOR);


        JLabel symbleJl = new JLabel("Symbol");
        symbleJF = new JTextField(20);
        symbleJF.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    searchOption();
                }
            }
        });


        JLabel mPC_UpwardTrendJl = new JLabel("MPC_UpwardTrend");
        mPC_UpwardTrendJF = new JTextField(20);
        mPC_UpwardTrendJF.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    searchOption();
                }
            }
        });


        JLabel mPC_DownWardTrendJl = new JLabel("MPC_DownWardTrend");
        mPC_DownWardTrendJF = new JTextField(20);
        mPC_DownWardTrendJF.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    searchOption();
                }
            }
        });


        JLabel m_U_TrendCountJl = new JLabel("M_U_TrendCount");
        m_U_TrendCountJF = new JTextField(20);
        m_U_TrendCountJF.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    searchOption();
                }
            }
        });


        JLabel m_D_TrendCountJl = new JLabel("M_D_TrendCount");
        m_D_TrendCountJF = new JTextField(20);
        m_D_TrendCountJF.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    searchOption();
                }
            }
        });


        JLabel t_U_T_DistanceJl = new JLabel("T_U_T_Distance");
        t_U_T_DistanceJF = new JTextField(20);
        t_U_T_DistanceJF.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    searchOption();
                }
            }
        });


        JLabel t_D_T_DistanceJl = new JLabel("T_D_T_Distance");
        t_D_T_DistanceJF = new JTextField(20);
        t_D_T_DistanceJF.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    searchOption();
                }
            }
        });


        JLabel volumeJl = new JLabel("Volume");
        volumeJF = new JTextField(20);
        volumeJF.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    searchOption();
                }
            }
        });


        JButton searchbtn = new JButton(" Search ");
        JButton clearbtn = new JButton(" Clear ");

        searchbtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                searchOption();
            }
        });
        clearbtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {

                symbleJF.setText(null);
                mPC_UpwardTrendJF.setText(null);
                mPC_DownWardTrendJF.setText(null);
                m_U_TrendCountJF.setText(null);
                m_D_TrendCountJF.setText(null);
                t_U_T_DistanceJF.setText(null);
                t_D_T_DistanceJF.setText(null);
                volumeJF.setText(null);
            }
        });


//Symble
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(symbleJl, gbc);

        gbc.gridx = 4;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridwidth = 2;
        panel.add(symbleJF, gbc);
//
        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(mPC_UpwardTrendJl, gbc);

        gbc.gridx = 2;
        gbc.gridy = 1;
        panel.add(mpcutComboList, gbc);

        gbc.gridx = 4;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridwidth = 2;
        panel.add(mPC_UpwardTrendJF, gbc);
//
        gbc.gridx = 0;
        gbc.gridy = 2;
        panel.add(mPC_DownWardTrendJl, gbc);

        gbc.gridx = 2;
        gbc.gridy = 2;
        panel.add(mpcdtComboList, gbc);

        gbc.gridx = 4;
        gbc.gridy = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridwidth = 2;
        panel.add(mPC_DownWardTrendJF, gbc);
//
        gbc.gridx = 0;
        gbc.gridy = 3;
        panel.add(m_U_TrendCountJl, gbc);

        gbc.gridx = 2;
        gbc.gridy = 3;
        panel.add(mutcComboList, gbc);

        gbc.gridx = 4;
        gbc.gridy = 3;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridwidth = 2;
        panel.add(m_U_TrendCountJF, gbc);
//
        gbc.gridx = 0;
        gbc.gridy = 4;
        panel.add(m_D_TrendCountJl, gbc);

        gbc.gridx = 2;
        gbc.gridy = 4;
        panel.add(mdtcComboList, gbc);

        gbc.gridx = 4;
        gbc.gridy = 4;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridwidth = 2;
        panel.add(m_D_TrendCountJF, gbc);
//
        gbc.gridx = 0;
        gbc.gridy = 5;
        panel.add(t_U_T_DistanceJl, gbc);

        gbc.gridx = 2;
        gbc.gridy = 5;
        panel.add(tutComboList, gbc);

        gbc.gridx = 4;
        gbc.gridy = 5;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridwidth = 2;
        panel.add(t_U_T_DistanceJF, gbc);

//
        gbc.gridx = 0;
        gbc.gridy = 6;
        panel.add(t_D_T_DistanceJl, gbc);

        gbc.gridx = 2;
        gbc.gridy = 6;
        panel.add(tdtComboList, gbc);

        gbc.gridx = 4;
        gbc.gridy = 6;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridwidth = 2;
        panel.add(t_D_T_DistanceJF, gbc);
//
        gbc.gridx = 0;
        gbc.gridy = 7;
        panel.add(volumeJl, gbc);

        gbc.gridx = 2;
        gbc.gridy = 7;
        panel.add(volumComboList, gbc);

        gbc.gridx = 4;
        gbc.gridy = 7;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridwidth = 2;
        panel.add(volumeJF, gbc);
//
        gbc.gridx = 0;
        gbc.gridy = 8;
        panel.add(searchbtn, gbc);

        gbc.gridx = 3;
        gbc.gridy = 8;
        panel.add(clearbtn, gbc);

        frame.getContentPane().add(panel, BorderLayout.CENTER);

        frame.setSize(450, 240);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
    }

    private void searchOption() {
        Map<String, String> map = new HashMap<String, String>();

        String symbletx = symbleJF.getText();
        if (!symbletx.isEmpty()) {
            map.put("symbol", symbletx.toUpperCase(Locale.ROOT));
        }

        String mPC_UpwardTrendtx = mPC_UpwardTrendJF.getText();
        if (!mPC_UpwardTrendtx.isEmpty()) {
            map.put("MPC_UpwardTrend", mPC_UpwardTrendtx);
        }

        String mPC_DownWardTrendtx = mPC_DownWardTrendJF.getText();
        if (!mPC_DownWardTrendtx.isEmpty()) {
            map.put("MPC_DownWardTrend", mPC_DownWardTrendtx);
        }

        String m_U_TrendCounttx = m_U_TrendCountJF.getText();
        if (!m_U_TrendCounttx.isEmpty()) {
            map.put("M_U_TrendCount", m_U_TrendCounttx);
        }

        String m_D_TrendCounttx = m_D_TrendCountJF.getText();
        if (!m_D_TrendCounttx.isEmpty()) {
            map.put("M_D_TrendCount", m_D_TrendCounttx);
        }

        String t_U_T_Distancetx = t_U_T_DistanceJF.getText();
        if (!t_U_T_Distancetx.isEmpty()) {
            map.put("T_U_T_Distance", t_U_T_Distancetx);
        }

        String t_D_T_Distancetx = t_D_T_DistanceJF.getText();
        if (!t_D_T_Distancetx.isEmpty()) {
            map.put("T_D_T_Distance", t_D_T_Distancetx);
        }

        String volumetx = volumeJF.getText();
        if (!volumetx.isEmpty()) {
            map.put("Volume", volumetx);
        }

        searchingData(map);
    }

    private void searchingData(Map<String, String> map) {

        List<StockTrendInfo> search = new ArrayList<>();

        for (StockTrendInfo sf : stockInfo) {

            boolean isFound = true;
            for (Map.Entry<String, String> entry : map.entrySet()) {

                if (entry.getKey().equals("symbol") && entry.getValue().length() != 0) {
                    if (!entry.getValue().equals(sf.getSymbol()))
                        isFound = false;
                }
                if (entry.getKey().equals("MPC_UpwardTrend") && entry.getValue().length() != 0) {
                    Double val = Double.parseDouble(entry.getValue());
                    int itemPosition = mpcutComboList.getSelectedIndex();
                    String comparator = MPCUTOPERATOR[itemPosition];

                    if (comparator.equals("<")) {
                        if (!(sf.getMaxPercentageChangeUpwardTrend() < val))
                            isFound = false;
                    }
                    if (comparator.equals(">")) {
                        if (!(sf.getMaxPercentageChangeUpwardTrend() > val))
                            isFound = false;
                    }
                    if (comparator.equals("==")) {
                        if (!(val == sf.getMaxPercentageChangeUpwardTrend()))
                            isFound = false;
                    }
                    if (comparator.equals(">=")) {
                        if (!(sf.getMaxPercentageChangeUpwardTrend() >= val))
                            isFound = false;
                    }
                    if (comparator.equals("<=")) {
                        if (!(sf.getMaxPercentageChangeUpwardTrend() <= val))
                            isFound = false;
                    }
                    if (comparator.equals("!=")) {
                        if (!(val != sf.getMaxPercentageChangeUpwardTrend()))
                            isFound = false;
                    }
                }
                if (entry.getKey().equals("MPC_DownWardTrend") && entry.getValue().length() != 0) {
                    Double val = Double.parseDouble(entry.getValue());
                    int itemPosition = mpcdtComboList.getSelectedIndex();
                    String comparator = MPCDTOPERATOR[itemPosition];

                    if (comparator.equals("<")) {
                        if (!(sf.getMaxPercentageChangeDownWardTrend() < val))
                            isFound = false;
                    }
                    if (comparator.equals(">")) {
                        if (!(sf.getMaxPercentageChangeDownWardTrend() > val))
                            isFound = false;
                    }
                    if (comparator.equals("==")) {
                        if (!(sf.getMaxPercentageChangeDownWardTrend() == val))
                            isFound = false;
                    }
                    if (comparator.equals(">=")) {
                        if (!(sf.getMaxPercentageChangeDownWardTrend() >= val))
                            isFound = false;
                    }
                    if (comparator.equals("<=")) {
                        if (!(sf.getMaxPercentageChangeDownWardTrend() <= val))
                            isFound = false;
                    }
                    if (comparator.equals("!=")) {
                        if (!(sf.getMaxPercentageChangeDownWardTrend() != val))
                            isFound = false;
                    }

                }
                if (entry.getKey().equals("M_U_TrendCount") && entry.getValue().length() != 0) {
                    int val = Integer.parseInt(entry.getValue());
                    int itemPosition = mutcComboList.getSelectedIndex();
                    String comparator = MUTCOPERATOR[itemPosition];

                    if (comparator.equals("<")) {
                        if (!(sf.getMaxUpwardTrendCount() < val))
                            isFound = false;
                    }
                    if (comparator.equals(">")) {
                        if (!(sf.getMaxUpwardTrendCount() > val))
                            isFound = false;
                    }
                    if (comparator.equals("==")) {
                        if (!(sf.getMaxUpwardTrendCount() == val))
                            isFound = false;
                    }
                    if (comparator.equals(">=")) {
                        if (!(sf.getMaxUpwardTrendCount() >= val))
                            isFound = false;
                    }
                    if (comparator.equals("<=")) {
                        if (!(sf.getMaxUpwardTrendCount() <= val))
                            isFound = false;
                    }
                    if (comparator.equals("!=")) {
                        if (sf.getMaxUpwardTrendCount() == val)
                            isFound = false;
                    }

                }
                if (entry.getKey().equals("M_D_TrendCount") && entry.getValue().length() != 0) {
                    int val = Integer.parseInt(entry.getValue());
                    int itemPosition = mdtcComboList.getSelectedIndex();
                    String comparator = MDTCOPERATOR[itemPosition];

                    if (comparator.equals("<")) {
                        if (!(sf.getMaxDownwardTrendCount() < val))
                            isFound = false;
                    }
                    if (comparator.equals(">")) {
                        if (!(sf.getMaxDownwardTrendCount() > val))
                            isFound = false;
                    }
                    if (comparator.equals("==")) {
                        if (!(sf.getMaxDownwardTrendCount() == val))
                            isFound = false;
                    }
                    if (comparator.equals(">=")) {
                        if (!(val >= sf.getMaxDownwardTrendCount()))
                            isFound = false;
                    }
                    if (comparator.equals("<=")) {
                        if (!(val <= sf.getMaxDownwardTrendCount()))
                            isFound = false;
                    }
                    if (comparator.equals("!=")) {
                        if (val == sf.getMaxDownwardTrendCount())
                            isFound = false;
                    }

                }
                if (entry.getKey().equals("T_U_T_Distance") && entry.getValue().length() != 0) {

                    int val = Integer.parseInt(entry.getValue());
                    int itemPosition = tutComboList.getSelectedIndex();
                    String comparator = TUTOPERATOR[itemPosition];

                    if (comparator.equals("<")) {
                        if (!(sf.getTotalUpwardTrendDistance() < val))
                            isFound = false;
                    }
                    if (comparator.equals(">")) {
                        if (!(sf.getTotalUpwardTrendDistance() > val))
                            isFound = false;
                    }
                    if (comparator.equals("==")) {
                        if (!(sf.getTotalUpwardTrendDistance() == val))
                            isFound = false;
                    }
                    if (comparator.equals(">=")) {
                        if (!(sf.getTotalUpwardTrendDistance() >= val))
                            isFound = false;
                    }
                    if (comparator.equals("<=")) {
                        if (!(sf.getTotalUpwardTrendDistance() <= val))
                            isFound = false;
                    }
                    if (comparator.equals("!=")) {
                        if (sf.getTotalUpwardTrendDistance() == val)
                            isFound = false;
                    }

                }
                if (entry.getKey().equals("T_D_T_Distance") && entry.getValue().length() != 0) {
                    int val = Integer.parseInt(entry.getValue());
                    int itemPosition = tdtComboList.getSelectedIndex();
                    String comparator = TDTOPERATOR[itemPosition];

                    if (comparator.equals("<")) {
                        if (!(sf.getTotalDownwardTrendDistance() < val))
                            isFound = false;
                    }
                    if (comparator.equals(">")) {
                        if (!(sf.getTotalDownwardTrendDistance() > val))
                            isFound = false;
                    }
                    if (comparator.equals("==")) {
                        if (!(sf.getTotalDownwardTrendDistance() == val))
                            isFound = false;
                    }
                    if (comparator.equals(">=")) {
                        if (!(sf.getTotalDownwardTrendDistance() >= val))
                            isFound = false;
                    }
                    if (comparator.equals("<=")) {
                        if (!(sf.getTotalDownwardTrendDistance() <= val))
                            isFound = false;
                    }
                    if (comparator.equals("!=")) {
                        if (sf.getTotalDownwardTrendDistance() == val)
                            isFound = false;
                    }

                }
                if (entry.getKey().equals("Volume") && entry.getValue().length() != 0) {
                    double val = Double.parseDouble(entry.getValue());
                    int itemPosition = volumComboList.getSelectedIndex();
                    String comparator = VOLUMOPERATOR[itemPosition];

                    if (comparator.equals("<")) {
                        if (!(sf.getVolumn() < val))
                            isFound = false;
                    }
                    if (comparator.equals(">")) {
                        if (!(sf.getVolumn() > val))
                            isFound = false;
                    }
                    if (comparator.equals("==")) {
                        if (!(sf.getVolumn() == val))
                            isFound = false;
                    }
                    if (comparator.equals(">=")) {
                        if (!(sf.getVolumn() >= val))
                            isFound = false;
                    }
                    if (comparator.equals("<=")) {
                        if (!(sf.getVolumn() <= val))
                            isFound = false;
                    }
                    if (comparator.equals("!=")) {
                        if (!(sf.getVolumn() != val))
                            isFound = false;
                    }

                }
            }

            if (isFound)
                search.add(sf);
        }
        tableModel = new StockTrendInfoTableModel(search);
        table.setModel(tableModel);
        //tablePanel.removeAll();
        tablePanel.add(sp, BorderLayout.CENTER);
        tablePanel.updateUI();
        table = tableAdjest(table);

    }


    private void showChart(Boolean logChart) {
        int[] selection = table.getSelectedRows();
        for (int i : selection) {
//          String selectedCellValue = (String) table.getValueAt(table.getSelectedRow(), FileUtils.getSymboleIndex());

            String selectedCellValue = (String) table.getValueAt(i, FileUtils.getSymboleIndex());
            mainPanel = new ShowChart().getChart(selectedCellValue, "OHLC Chart Acceleration Count Asc", stockInfo, CandleChartsEnum.UPWARD_TREND_COUNT_ASC, 0, logChart, true);

            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            double width = screenSize.getWidth();
            double height = screenSize.getHeight();

            jFrame = new JFrame();
            jFrame.add(mainPanel);
            jFrame.setVisible(true);
            jFrame.setSize((int) width, (int) height);
            jFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);


        }
    }


    private JTable tableAdjest(JTable table) {
        table.getColumnModel().getColumn(0).setPreferredWidth(50);
        table.getColumnModel().getColumn(1).setPreferredWidth(100);
        table.getColumnModel().getColumn(2).setPreferredWidth(170);
        table.getColumnModel().getColumn(3).setPreferredWidth(170);
        table.getColumnModel().getColumn(4).setPreferredWidth(170);
        table.getColumnModel().getColumn(5).setPreferredWidth(180);
        table.getColumnModel().getColumn(6).setPreferredWidth(170);
        table.getColumnModel().getColumn(7).setPreferredWidth(170);
        table.getColumnModel().getColumn(8).setPreferredWidth(170);
        table.getColumnModel().getColumn(9).setPreferredWidth(180);
        table.getColumnModel().getColumn(10).setPreferredWidth(170);
        table.getColumnModel().getColumn(11).setPreferredWidth(170);
        table.getColumnModel().getColumn(12).setPreferredWidth(180);
        table.getColumnModel().getColumn(13).setPreferredWidth(180);
        table.getColumnModel().getColumn(14).setPreferredWidth(100);
        table.getColumnModel().getColumn(15).setPreferredWidth(100);
        table.getColumnModel().getColumn(16).setPreferredWidth(100);
        table.getColumnModel().getColumn(17).setPreferredWidth(100);
        //table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        return table;
    }
}
