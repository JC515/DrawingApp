import Shape.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

// 画图应用主类，继承自JFrame
public class DrawingApp extends JFrame {
    private final DrawingBoard drawingBoard = new DrawingBoard(); // 画板
    private final Stack<Memento> undoStack = new Stack<>(); // 撤销栈
    private final Stack<Memento> redoStack = new Stack<>(); // 重做栈
    private final List<Point> freeDrawPoints = new ArrayList<>(); // 自由图形的点集
    private String currentTool = "自由图形"; // 当前工具
    private Color currentColor = Color.BLACK; // 当前颜色
    private Point startPoint; // 开始点
    private Point endPoint; // 结束点
    private boolean isDragging = false; // 是否正在拖动
    private float currentStrokeWidth = 1.0f;  // 默认线条粗细
    private TempShape tempShape; // 临时图形

    // 构造函数，初始化画图应用
    public DrawingApp() {
        init();
        initMenu();
        registerKeyboardShortcuts();
    }

    // 初始化菜单
    private void initMenu() {
        JMenuBar menuBar = new JMenuBar();
        JMenu shapeMenu = new JMenu("选择图形");
        String[] tools = {"自由图形", "长方形", "圆形", "直线"};
        for (String tool : tools) {
            JMenuItem item = new JMenuItem(tool);
            item.addActionListener(e -> {
                currentTool = tool;
                if (!currentTool.equals("自由图形")) {
                    freeDrawPoints.clear();
                }
                repaint();
            });
            shapeMenu.add(item);
        }
        menuBar.add(shapeMenu);

        JMenu settingsMenu = getjMenu();
        menuBar.add(settingsMenu);

        JMenu functionMenu = getFunctionMenu();
        menuBar.add(functionMenu);

        setJMenuBar(menuBar);
    }


    // 设置界面基础参数
    private void init() {
        setTitle("简易画图板");
        setSize(800, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        setLocationRelativeTo(null);
        JPanel canvas = getPanel();
        add(canvas, BorderLayout.CENTER);
    }

    private JMenu getjMenu() {
        JMenu settingsMenu = new JMenu("设置");
        JMenuItem colorItem = new JMenuItem("设置颜色");
        colorItem.addActionListener(e -> {
            Color newColor = JColorChooser.showDialog(DrawingApp.this, "Choose a color", currentColor);
            if (newColor != null) {
                currentColor = newColor;
            }
        });

        // 添加设置线条粗细的滑条
        JMenuItem strokeWidthItem = getjMenuItem();

        settingsMenu.add(colorItem);
        settingsMenu.add(strokeWidthItem);
        return settingsMenu;
    }

    private JMenuItem getjMenuItem() {
        JMenuItem strokeWidthItem = new JMenuItem("设置线条粗细");
        strokeWidthItem.addActionListener(e -> {
            JSlider slider = new JSlider(1, 10, (int) currentStrokeWidth);
            slider.setMajorTickSpacing(1);
            slider.setPaintTicks(true);
            slider.setPaintLabels(true);
            slider.addChangeListener(e1 -> currentStrokeWidth = slider.getValue());
            JOptionPane.showMessageDialog(DrawingApp.this, slider, "调整线条粗细", JOptionPane.PLAIN_MESSAGE);
        });
        return strokeWidthItem;
    }

    // 获取功能菜单
    private JMenu getFunctionMenu() {
        JMenu functionMenu = new JMenu("功能");
        JMenuItem undoItem = new JMenuItem("撤销");
        undoItem.addActionListener(e -> undo());
        functionMenu.add(undoItem);

        JMenuItem redoItem = new JMenuItem("恢复");
        redoItem.addActionListener(e -> redo());
        functionMenu.add(redoItem);

        JMenuItem clearItem = new JMenuItem("清空");
        clearItem.addActionListener(e -> {
            saveState();
            drawingBoard.clearShapes();
            freeDrawPoints.clear();
            repaint();
        });
        functionMenu.add(clearItem);
        return functionMenu;
    }

    // 注册键盘快捷键
    private void registerKeyboardShortcuts() {
        KeyStroke undoKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_Z, KeyEvent.CTRL_DOWN_MASK);
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(undoKeyStroke, "undo");
        getRootPane().getActionMap().put("undo", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                undo();
            }
        });

        KeyStroke redoKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_Y, KeyEvent.CTRL_DOWN_MASK);
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(redoKeyStroke, "redo");
        getRootPane().getActionMap().put("redo", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                redo();
            }
        });

        KeyStroke clearKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_C, KeyEvent.CTRL_DOWN_MASK);
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(clearKeyStroke, "clear");
        getRootPane().getActionMap().put("clear", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                clear();
            }
        });
    }

    // 获取画布面板
    private JPanel getPanel() {
        JPanel canvas = getCanvas();
        canvas.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (currentTool.equals("自由图形")) {
                    freeDrawPoints.clear();
                    freeDrawPoints.add(e.getPoint());
                    isDragging = true;
                    startPoint = e.getPoint();
                } else {
                    startPoint = e.getPoint();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (!isDragging) {
                    freeDrawPoints.clear();
                    startPoint = null;
                    endPoint = null;
                    repaint();
                    return;
                }

                saveState();
                switch (currentTool) {
                    case "自由图形" -> {
                        drawingBoard.addShape(new FreeDrawMyShape(freeDrawPoints, currentStrokeWidth, currentColor));
                        freeDrawPoints.clear();
                    }
                    case "圆形" -> {
                        MyShape circle = createCircle(startPoint, e.getPoint());
                        drawingBoard.addShape(circle);
                    }
                    case "长方形" -> {
                        MyShape rectangle = createRectangle(startPoint, e.getPoint());
                        drawingBoard.addShape(rectangle);
                    }
                    case "直线" -> {
                        MyShape line = createLine(startPoint, e.getPoint());
                        drawingBoard.addShape(line);
                    }
                }
                isDragging = false;
                repaint();
            }
        });

        canvas.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                isDragging = true;
                endPoint = e.getPoint(); // 更新终点
                if (currentTool.equals("自由图形")) {
                    freeDrawPoints.add(endPoint);
                    repaint();
                } else {
                    repaint(); // 直接重绘
                }
            }
        });
        return canvas;
    }

    // 创建直线形状
    private MyShape createLine(Point start, Point end) {
        return new LineMyShape(start.x, start.y, end.x, end.y, currentColor, currentStrokeWidth);
    }

    // 获取画布
    private JPanel getCanvas() {
        JPanel canvas = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;  // 转换为Graphics2D
                g2d.setStroke(new BasicStroke(currentStrokeWidth));  // 设置线条粗细

                // 绘制已添加的形状
                for (MyShape shape : drawingBoard.getShapes()) {
                    shape.draw(g); // 此处可以考虑将线条粗细带到Shape类中
                }

                // 绘制当前自由画的路径
                if (currentTool.equals("自由图形")) {
                    g2d.setColor(currentColor);
                    g2d.setStroke(new BasicStroke(currentStrokeWidth));
                    for (int i = 1; i < freeDrawPoints.size(); i++) {
                        Point p1 = freeDrawPoints.get(i - 1);
                        Point p2 = freeDrawPoints.get(i);
                        g2d.drawLine(p1.x, p1.y, p2.x, p2.y);
                    }
                }

                // 实时绘制当前形状
                if (startPoint != null && endPoint != null && isDragging) {
                    switch (currentTool) {
                        case "圆形" -> {
                            int radius = (int) startPoint.distance(endPoint) / 2;
                            int centerX = (startPoint.x + endPoint.x) / 2;
                            int centerY = (startPoint.y + endPoint.y) / 2;
                            tempShape = new TempShape(new CircleMyShape(centerX, centerY, radius, currentColor, currentStrokeWidth), currentColor, true);
                            tempShape.shape.draw(g);
                        }
                        case "长方形" -> {
                            int x = Math.min(startPoint.x, endPoint.x);
                            int y = Math.min(startPoint.y, endPoint.y);
                            int width = Math.abs(startPoint.x - endPoint.x);
                            int height = Math.abs(startPoint.y - endPoint.y);
                            tempShape = new TempShape(new RectangleMyShape(x, y, width, height, currentColor, currentStrokeWidth), currentColor, true);
                            tempShape.shape.draw(g);
                        }
                        case "直线" -> {
                            g2d.setColor(currentColor);
                            g2d.setStroke(new BasicStroke(currentStrokeWidth));
                            g2d.drawLine(startPoint.x, startPoint.y, endPoint.x, endPoint.y); // 绘制直线
                        }
                    }
                }
            }
        };
        canvas.setBackground(Color.WHITE);
        return canvas;
    }


    // 创建圆形形状
    private MyShape createCircle(Point start, Point end) {
        int radius = (int) start.distance(end) / 2;
        int centerX = (start.x + end.x) / 2;
        int centerY = (start.y + end.y) / 2;
        return new CircleMyShape(centerX, centerY, radius, currentColor, currentStrokeWidth);
    }

    // 创建矩形形状
    private MyShape createRectangle(Point start, Point end) {
        int x = Math.min(start.x, end.x);
        int y = Math.min(start.y, end.y);
        int width = Math.abs(start.x - end.x);
        int height = Math.abs(start.y - end.y);
        return new RectangleMyShape(x, y, width, height, currentColor, currentStrokeWidth);
    }

    // 保存当前状态到撤销栈
    private void saveState() {
        undoStack.push(drawingBoard.save());
        redoStack.clear();
    }

    // 撤销上一个操作
    private void undo() {
        if (!undoStack.isEmpty()) {
            redoStack.push(drawingBoard.save());
            Memento undoState = undoStack.pop();
            drawingBoard.restore(undoState);
            repaint();
        } else {
            JOptionPane.showMessageDialog(this, "没有可撤销的操作！", "提示", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    // 恢复上一个撤销的操作
    private void redo() {
        if (!redoStack.isEmpty()) {
            undoStack.push(drawingBoard.save());
            Memento redoState = redoStack.pop();
            drawingBoard.restore(redoState);
            repaint();
        } else {
            JOptionPane.showMessageDialog(this, "没有可恢复的操作！", "提示", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    // 清空画布
    private void clear() {
        saveState();
        drawingBoard.clearShapes();
        freeDrawPoints.clear();
        repaint();
    }
}
