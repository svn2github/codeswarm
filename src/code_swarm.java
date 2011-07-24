/**
 * Copyright 2008 Michael Ogawa
 *
 * This file is part of code_swarm.
 *
 * code_swarm is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * code_swarm is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with code_swarm.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Properties;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.awt.Color;

import javax.vecmath.Vector2f;
import org.codeswarm.dependencies.sun.tools.javac.util.Pair;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

import processing.core.PApplet;
import processing.core.PFont;
import processing.core.PImage;

/**
 *
 *
 */
public class code_swarm extends PApplet {
  /** @remark needed for any serializable class */
  public static final long serialVersionUID = 0;

  // User-defined variables
  int FRAME_RATE = 24;
  long UPDATE_DELTA = -1;
  String SPRITE_FILE = "particle.png";
  String SCREENSHOT_FILE;
  int background;

  // Data storage
  BlockingQueue<FileEvent> eventsQueue;
  boolean isInputSorted = false;
  protected Map<String, FileNode> nodes;
  protected Map<Pair<FileNode, PersonNode>, Edge> edges;
  protected Map<String, PersonNode> people;

  // Liveness cache
  LinkedList<PersonNode> livingPeople = new LinkedList<PersonNode>();
  LinkedList<Edge> livingEdges = new LinkedList<Edge>();
  LinkedList<FileNode> livingNodes = new LinkedList<FileNode>(); 
  
  LinkedList<ColorBins> history;
  LinkedList<Integer> peopleHistory;

  boolean finishedLoading = false;

  // Temporary variables
  FileEvent currentEvent;
  Date nextDate;
  Date prevDate;
  FileNode prevNode;
  int maxTouches;

  // Graphics objects
  PFont font;
  PFont boldFont;
  PFont infoFont;
  PImage sprite;

  // Graphics state variables
  boolean looping = true;
  boolean coolDown = false;
  boolean showHistogram = true;
  boolean showActivity = false;
  boolean showDate = true;
  boolean showLegend = false;
  boolean showPopular = false;
  boolean showEdges = false;
  boolean showEngine = false;
  boolean showHelp = false;
  boolean takeSnapshots = false;
  boolean showDebug = false;
  boolean drawNamesSharp = false;
  boolean drawNamesHalos = false;
  boolean drawFilesSharp = false;
  boolean drawFilesFuzzy = false;
  boolean drawFilesJelly = false;

  //used to ensure that input is sorted when we are told it is
  long maximumDateSeenSoFar = 0;

  // Color mapper
  ColorAssigner colorAssigner;
  int currentColor;

  // Edge Length
  protected static int EDGE_LEN = 25;
  // Drawable object life decrement
  private int EDGE_LIFE_INIT = 255;
  private int FILE_LIFE_INIT = 255;
  private int PERSON_LIFE_INIT = 255;
  private int EDGE_LIFE_DECREMENT = -1;
  private int FILE_LIFE_DECREMENT = -1;
  private int PERSON_LIFE_DECREMENT = -1;

  private float DEFAULT_NODE_SPEED = 7.0f;
  private float DEFAULT_FILE_SPEED = 7.0f;
  private float DEFAULT_PERSON_SPEED = 2.0f;

  private float FILE_MASS = 1.0f;
  private float PERSON_MASS = 10.0f;

  private int HIGHLIGHT_PCT = 5;

  // Physics engine configuration
  String          physicsEngineConfigDir;
  String          physicsEngineSelection;
  LinkedList<peConfig> mPhysicsEngineChoices = new LinkedList<peConfig>();
  PhysicsEngine  mPhysicsEngine = null;
  private boolean safeToToggle = false;
  private boolean wantToToggle = false;
  private boolean toggleDirection = false;


  // Default Physics Engine (class) name
  static final String PHYSICS_ENGINE_LEGACY  = "PhysicsEngineLegacy";

  // Formats the date string nicely
  DateFormat formatter = DateFormat.getDateInstance();

  protected static CodeSwarmConfig cfg;
  private long lastDrawDuration = 0;
  private String loadingMessage = "Reading input file";
  protected static int width=0;
  protected static int height=0;
  private int maxFramesSaved;

  protected int maxBackgroundThreads;
  protected ExecutorService backgroundExecutor;
  /**
   *  Used for utility functions
   *  current members:
   *      drawPoint: Pass coords and color
   *      drawLine: Pass coords and color
   */
  public static Utils utils = null;

  /**
   * Initialization
   */
  public void setup() {

    utils = new Utils();

    width=cfg.getIntProperty(CodeSwarmConfig.WIDTH_KEY,640);

    if (width <= 0) {
      width = 640;
    }

    height=cfg.getIntProperty(CodeSwarmConfig.HEIGHT_KEY,480);
    if (height <= 0) {
      height = 480;
    }

    maxBackgroundThreads=cfg.getIntProperty(CodeSwarmConfig.MAX_THREADS_KEY,4);
    if (maxBackgroundThreads <= 0) {
      maxBackgroundThreads = 4;
    }
    backgroundExecutor = new ThreadPoolExecutor(1, maxBackgroundThreads, Long.MAX_VALUE, TimeUnit.NANOSECONDS, new ArrayBlockingQueue<Runnable>(4 * maxBackgroundThreads), new ThreadPoolExecutor.CallerRunsPolicy());

    if (cfg.getBooleanProperty(CodeSwarmConfig.USE_OPEN_GL, false)) {
      size(width, height, OPENGL);
    } else {
      size(width, height);
    }

    if (cfg.getBooleanProperty(CodeSwarmConfig.SHOW_LEGEND, false)) {
      showLegend = true;
    } else {
      showLegend = false;
    }

    if (cfg.getBooleanProperty(CodeSwarmConfig.SHOW_HISTORY, false)) {
      showHistogram = true;
    } else {
      showHistogram = false;
    }

    if (cfg.getBooleanProperty(CodeSwarmConfig.SHOW_ACTIVITY, false)) {
      showActivity = true;
    } else {
      showActivity = false;
    }

    if (cfg.getBooleanProperty(CodeSwarmConfig.SHOW_DATE, false)) {
      showDate = true;
    } else {
      showDate = false;
    }

    if (cfg.getBooleanProperty(CodeSwarmConfig.SHOW_EDGES, false)) {
      showEdges = true;
    } else {
      showEdges = false;
    }

    if (cfg.getBooleanProperty(CodeSwarmConfig.SHOW_DEBUG, false)) {
      showDebug = true;
    } else {
      showDebug = false;
    }

    if (cfg.getBooleanProperty(CodeSwarmConfig.TAKE_SNAPSHOTS_KEY,false)) {
      takeSnapshots = true;
    } else {
      takeSnapshots = false;
    }

    if (cfg.getBooleanProperty(CodeSwarmConfig.DRAW_NAMES_SHARP, true)) {
      drawNamesSharp = true;
    } else {
      drawNamesSharp = false;
    }

    if (cfg.getBooleanProperty(CodeSwarmConfig.DRAW_NAMES_HALOS, false)) {
      drawNamesHalos = true;
    } else {
      drawNamesHalos = false;
    }

    if (cfg.getBooleanProperty(CodeSwarmConfig.DRAW_FILES_SHARP, false)) {
      drawFilesSharp = true;
    } else {
      drawFilesSharp = false;
    }

    if (cfg.getBooleanProperty(CodeSwarmConfig.DRAW_FILES_FUZZY, true)) {
      drawFilesFuzzy = true;
    } else {
      drawFilesFuzzy = false;
    }

    if (cfg.getBooleanProperty(CodeSwarmConfig.DRAW_FILES_JELLY, false)) {
      drawFilesJelly = true;
    } else {
      drawFilesJelly = false;
    }

    background = cfg.getBackground().getRGB();

    // Ensure we have sane values.
    EDGE_LIFE_INIT = cfg.getIntProperty(CodeSwarmConfig.EDGE_LIFE_KEY,255);
    if (EDGE_LIFE_INIT <= 0) {
      EDGE_LIFE_INIT = 255;
    }

    FILE_LIFE_INIT = cfg.getIntProperty(CodeSwarmConfig.FILE_LIFE_KEY,255);
    if (FILE_LIFE_INIT <= 0) {
      FILE_LIFE_INIT = 255;
    }

    PERSON_LIFE_INIT = cfg.getIntProperty(CodeSwarmConfig.PERSON_LIFE_KEY,255);
    if (PERSON_LIFE_INIT <= 0) {
      PERSON_LIFE_INIT = 255;
    }

    /* enforce decrements < 0 */
    EDGE_LIFE_DECREMENT = cfg.getIntProperty(CodeSwarmConfig.EDGE_DECREMENT_KEY,-2);
    if (EDGE_LIFE_DECREMENT >= 0) {
      EDGE_LIFE_DECREMENT = -2;
    }
    FILE_LIFE_DECREMENT = cfg.getIntProperty(CodeSwarmConfig.FILE_DECREMENT_KEY,-2);
    if (FILE_LIFE_DECREMENT >= 0) {
      FILE_LIFE_DECREMENT = -2;
    }
    PERSON_LIFE_DECREMENT = cfg.getIntProperty(CodeSwarmConfig.PERSON_DECREMENT_KEY,-1);
    if (PERSON_LIFE_DECREMENT >= 0) {
      PERSON_LIFE_DECREMENT = -1;
    }

    DEFAULT_NODE_SPEED = cfg.getFloatProperty(CodeSwarmConfig.NODE_SPEED_KEY, 7.0f);
    DEFAULT_FILE_SPEED = cfg.getFloatProperty(CodeSwarmConfig.FILE_SPEED_KEY, DEFAULT_NODE_SPEED);
    DEFAULT_PERSON_SPEED = cfg.getFloatProperty(CodeSwarmConfig.PERSON_SPEED_KEY, DEFAULT_NODE_SPEED);

    FILE_MASS = cfg.getFloatProperty(CodeSwarmConfig.FILE_MASS_KEY,1.0f);
    PERSON_MASS = cfg.getFloatProperty(CodeSwarmConfig.PERSON_MASS_KEY,1.0f);

    HIGHLIGHT_PCT = cfg.getIntProperty(CodeSwarmConfig.HIGHLIGHT_PCT_KEY,5);
    if (HIGHLIGHT_PCT < 0 || HIGHLIGHT_PCT > 100) {
      HIGHLIGHT_PCT = 5;
    }

    UPDATE_DELTA = cfg.getIntProperty(CodeSwarmConfig.MSEC_PER_FRAME_KEY, -1);
    if (UPDATE_DELTA == -1) {
      int framesperday = cfg.getIntProperty(CodeSwarmConfig.FRAMES_PER_DAY_KEY, 4);
      if (framesperday > 0) {
        UPDATE_DELTA = (86400000 / framesperday);
      }
    }
    if (UPDATE_DELTA <= 0) {
      // Default to 4 frames per day.
      UPDATE_DELTA = 21600000;
    }

    isInputSorted = cfg.getBooleanProperty(CodeSwarmConfig.IS_INPUT_SORTED_KEY, false);

    /**
     * This section loads config files and calls the setup method for all physics engines.
     */

    physicsEngineConfigDir = cfg.getStringProperty( CodeSwarmConfig.PHYSICS_ENGINE_CONF_DIR, "physics_engine");
    File f = new File(physicsEngineConfigDir);
    String[] configFiles = null;
    if ( f.exists()  &&  f.isDirectory() ) {
      configFiles = f.list();
    }
    for (int i=0; configFiles != null  &&  i<configFiles.length; i++) {
      if (configFiles[i].endsWith(".config")) {
        Properties p = new Properties();
        String ConfigPath = physicsEngineConfigDir + System.getProperty("file.separator") + configFiles[i];
        try {
          p.load(new FileInputStream(ConfigPath));
        } catch (FileNotFoundException e) {
          e.printStackTrace();
          System.exit(1);
        } catch (IOException e) {
          e.printStackTrace();
          System.exit(1);
        }
        String ClassName = p.getProperty("name", "__DEFAULT__");
        if ( ! ClassName.equals("__DEFAULT__")) {
          PhysicsEngine pe = getPhysicsEngine(ClassName);
          pe.setup(this, p);
          peConfig pec = new peConfig(ClassName,pe);
          mPhysicsEngineChoices.add(pec);
        } else {
          System.out.println("Skipping config file '" + ConfigPath + "'.  Must specify class name via the 'name' parameter.");
          System.exit(1);
        }
      }
    }

    if (mPhysicsEngineChoices.size() == 0) {
      System.out.println("No physics engine config files found in '" + physicsEngineConfigDir + "'.");
      System.exit(1);
    }

    // Physics engine configuration and instantiation
    physicsEngineSelection = cfg.getStringProperty( CodeSwarmConfig.PHYSICS_ENGINE_SELECTION, PHYSICS_ENGINE_LEGACY );

    for (peConfig p : mPhysicsEngineChoices) {
      if (physicsEngineSelection.equals(p.name)) {
        mPhysicsEngine = p.pe;
      }
    }

    if (mPhysicsEngine == null) {
      System.out.println("No physics engine matches your choice of '" + physicsEngineSelection + "'. Check '" + physicsEngineConfigDir + "' for options.");
      System.exit(1);
    }

    smooth();
    frameRate(FRAME_RATE);

    // init data structures
    nodes         = new HashMap<String,FileNode>();
    edges         = new HashMap<Pair<FileNode, PersonNode>, Edge>();
    people        = new HashMap<String,PersonNode>();
    history       = new LinkedList<ColorBins>(); 
    peopleHistory = new LinkedList<Integer>(); 
    
    if (isInputSorted) {
      //If the input is sorted, we only need to store the next few events
      eventsQueue = new ArrayBlockingQueue<FileEvent>(5000);
    } else {
      //Otherwise we need to store them all at once in a data structure that will sort them
      eventsQueue = new PriorityBlockingQueue<FileEvent>();
    }

    // Init color map
    initColors();

    loadRepEvents(cfg.getStringProperty(CodeSwarmConfig.INPUT_FILE_KEY)); // event formatted (this is the standard)
    while (!finishedLoading && eventsQueue.isEmpty());
    prevDate = eventsQueue.peek().date;

    SCREENSHOT_FILE = cfg.getStringProperty(CodeSwarmConfig.SNAPSHOT_LOCATION_KEY);

    maxFramesSaved = (int) Math.pow(10, SCREENSHOT_FILE.replaceAll("[^#]","").length());

    EDGE_LEN = cfg.getIntProperty(CodeSwarmConfig.EDGE_LENGTH_KEY);
    if (EDGE_LEN <= 0) {
      EDGE_LEN = 25;
    }

    // Create fonts
    String fontName = cfg.getStringProperty(CodeSwarmConfig.FONT_KEY,"SansSerif");
    String fontNameBold = cfg.getStringProperty(CodeSwarmConfig.FONT_KEY_BOLD,"SansSerif");
    String fontNameInfo = cfg.getStringProperty(CodeSwarmConfig.FONT_KEY_INFO,"SansSerif");
    Integer fontSize = cfg.getIntProperty(CodeSwarmConfig.FONT_SIZE, 10);
    Integer fontSizeBold = cfg.getIntProperty(CodeSwarmConfig.FONT_SIZE_BOLD, 14);
    Integer fontSizeInfo = cfg.getIntProperty(CodeSwarmConfig.FONT_SIZE_INFO, 20);
    font = createFont(fontName, fontSize);
    boldFont = createFont(fontNameBold, fontSizeBold);
    infoFont = createFont(fontNameInfo,fontSizeInfo);

    textFont(font);

    String SPRITE_FILE = cfg.getStringProperty(CodeSwarmConfig.SPRITE_FILE_KEY);
    // Create the file particle image
    sprite = loadImage(SPRITE_FILE);
    // Add translucency (using itself in this case)
    sprite.mask(sprite);
  }

  /**
   * Load a colormap
   */
  public void initColors() {
    colorAssigner = new ColorAssigner();
    int i = 1;
    String property;
    while ((property = cfg.getColorAssignProperty(i)) != null) {
      ColorTest ct = new ColorTest();
      ct.loadProperty(property);
      colorAssigner.addRule(ct);
      i++;
    }
    // Load the default.
    ColorTest ct = new ColorTest();
    ct.loadProperty(CodeSwarmConfig.DEFAULT_COLOR_ASSIGN);
    colorAssigner.addRule(ct);
  }

  /**
   * Main loop
   */
  public void draw() {
    long start = System.currentTimeMillis();
    background(background); // clear screen with background color

    this.update(); // update state to next frame

    // Draw edges (for debugging only)
    if (showEdges) {
      for (Edge edge : edges.values()) {
        edge.draw();
      }
    }

    // Surround names with aura
    // Then blur it
    if (drawNamesHalos) {
      drawPeopleNodesBlur();
    }

    // Then draw names again, but sharp
    if (drawNamesSharp) {
      drawPeopleNodesSharp();
    }

    // Draw file particles
    for (FileNode node : livingNodes) {
      node.draw();
    }

    textFont(font);

    // Show the physics engine name
    if (showEngine) {
      drawEngine();
    }

    // help, legend and debug information are exclusive
    if (showHelp) {
      // help override legend and debug information
      drawHelp();
    }
    else if (showDebug) {
      // debug override legend information
      drawDebugData();
    }
    else if (showLegend) {
      // legend only if nothing "more important"
      drawLegend();
    }

    if (showPopular) {
      drawPopular();
    }

    if (showHistogram) {
      drawHistory();
    }
    
    if (showActivity) {
	drawPeopleHistory();
    }

    if (showDate) {
      drawDate();
    }

    if (takeSnapshots) {
      dumpFrame();
    }

    // Stop animation when we run out of data AND all nodes are dead
    if (eventsQueue.isEmpty()) {
      coolDown = true;
      if ( !isThereLife() ) {
        // noLoop();
        backgroundExecutor.shutdown();
        try {
          backgroundExecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
        } catch (InterruptedException e) { /* Do nothing, just exit */}
        exit();
      }
    }

    long end = System.currentTimeMillis();
    lastDrawDuration = end - start;
  }

  /**
   * The Physics engines may need access to the nodes
   * in calls other than onUpdate*() and onRelax*()
   */
  public LinkedList<PersonNode> getLivingPeople() {
      return livingPeople;
  }
  public LinkedList<Edge> getLivingEdges() {
      return livingEdges;
  }
  public LinkedList<FileNode> getLivingNodes() {
      return livingNodes;
  }

  /**
   * Surround names with aura
   */
  public void drawPeopleNodesBlur() {
    colorMode(HSB);
    // First draw the name
    for (PersonNode p : livingPeople) {
      fill(hue(p.flavor), 64, 255, p.life);
      p.draw();
    }

    // Then blur it
    filter(BLUR, 3);
  }

  /**
   * Draw person's name
   */
  public void drawPeopleNodesSharp() {
    colorMode(RGB);
    for (PersonNode p : livingPeople) {
      fill(lerpColor(p.flavor, color(255), 0.5f), max(p.life - 50, 0));
      p.draw();
    }
  }

  /**
   * Draw date in lower-right corner
   */
  public void drawDate() {
    fill(255);
    String dateText = formatter.format(prevDate);
    textFont(infoFont);
    textAlign(RIGHT, BASELINE);
    textSize(infoFont.size);
    if (coolDown)
      dateText = "End of history: " + dateText;
    text(dateText, width - 1 - 10, height - textDescent() - 10);
  }

  /**
   *  Draw histogram in lower-left
   */
  public void drawHistory() {
     int x = 0;
     int heightMinusThree = height - 3;
     rectMode(CORNERS);

     for (ColorBins cb : history) {
        if (cb.totalCount > 0) {
           int startY = 0;
           int endY = 0;
           for (int k = 0; k < cb.keyCount; ++k) {
              int color = cb.keys[k];
              endY += cb.colorMap.get(color);
              stroke(color, 255); // 200 (nicer) vs. 255 (faster)
              rect(x, heightMinusThree - startY, x, heightMinusThree - endY);
              startY = endY;
           }
        }
        x++;
     }
  }

  /**
   * Draw active committers at bottom
   */
  public void drawPeopleHistory() {
      int x = width / 2 - peopleHistory.size() / 2 ;
     int heightMinusThree = height - 3;
     rectMode(CORNERS);

     for (int people : peopleHistory) {
	 // clamp people to 1 to 500 as a reasonable interval for most projects
	 // ln x where x is 1 to 500
	 // ln 1 = 0
	 // ln 500 = 6.215   
	 // 
	 // Graph height wanted: 20 to 250
	 // 230 / 6.215 = 37.01
	 // 20 + ln x * 37.01
	 final int GRAPH_HEIGHT_MIN = 10;
	 final int GRAPH_HEIGHT_MAX = 70;
	 final int MIN_PEOPLE = 1;
	 final int MAX_PEOPLE = 500;
	 final double LOG_SCALE = ( GRAPH_HEIGHT_MAX - GRAPH_HEIGHT_MIN ) / (double)Math.log( MAX_PEOPLE );
	 int peopleNormalized = max( MIN_PEOPLE, people, min( people, MAX_PEOPLE ) ); 
	 int heightLog = GRAPH_HEIGHT_MIN * max( min( 1, people ), 0 ) + (int)( Math.log( peopleNormalized ) * LOG_SCALE );
	 //System.out.print( "people: " + people + " norm people: " + peopleNormalized + " heightLog " + heightLog + "\n" );
	 int startY = heightMinusThree - heightLog;
	 int endY = heightMinusThree;
	 int color = new Color( 128, 128, 128 ).getRGB();
	 stroke(color, 255); // 200 (nicer) vs. 255 (faster)
	 rect(x, startY, x, endY);
	 x++;
     }
  }

  /**
   * Show the Loading screen.
   */

  public void drawLoading() {
    noStroke();
    textFont(font, 20);
    textAlign(LEFT, TOP);
    fill(255, 200);
    text(loadingMessage, 0, 0);
  }

  /**
   *  Show color codings
   */
  public void drawLegend() {
    noStroke();
    textFont(infoFont);
    textAlign(LEFT, TOP);
    fill(255, 200);
    text("Legend:", 10, 10);
    for (int i = 0; i < colorAssigner.tests.size(); i++) {
      ColorTest t = colorAssigner.tests.get(i);
      fill(t.c1, 200);
      text(t.label, 10 + infoFont.size, (i + 1) * infoFont.size + 10);
    }
  }

  /**
   *  Show physics engine name
   */
  public void drawEngine() {
    fill(255);
    textAlign(RIGHT, BASELINE);
    textSize(10);
    text(physicsEngineSelection, width-1, height - (textDescent() * 5));
  }

  /**
   *  Show short help on available commands
   */
  public void drawHelp() {
    int line = 0;
    noStroke();
    textFont(font);
    textAlign(LEFT, TOP);
    fill(255, 200);
    text("Help on keyboard commands:", 0, 10*line++);
    text("space bar : pause", 0, 10*line++);
    text("           a : show name halos", 0, 10*line++);
    text("           b : show debug", 0, 10*line++);
    text("           d : show date", 0, 10*line++);
    text("           e : show edges", 0, 10*line++);
    text("           E : show physics engine name", 0, 10*line++);
    text("            f : draw files fuzzy", 0, 10*line++);
    text("           h : show histogram", 0, 10*line++);
    text("            j : draw files jelly", 0, 10*line++);
    text("            l : show legend", 0, 10*line++);
    text("           p : show popular", 0, 10*line++);
    text("           q : quit code_swarm", 0, 10*line++);
    text("           s : draw names sharp", 0, 10*line++);
    text("           S : draw files sharp", 0, 10*line++);
    text("   minus : previous physics engine", 0, 10*line++);
    text("      plus : next physics engine", 0, 10*line++);
    text("           ? : show help", 0, 10*line++);
  }
  /**
   *  Show debug information about all drawable objects
   */
  public void drawDebugData() {
    noStroke();
    textFont(font);
    textAlign(LEFT, TOP);
    fill(255, 200);
    text("Nodes: " + nodes.size(), 0, 0);
    text("People: " + people.size(), 0, 10);
    text("Queue: " + eventsQueue.size(), 0, 20);
    text("Last render time: " + lastDrawDuration, 0, 30);
  }

  /**
   * TODO This could be made to look a lot better.
   */
  public void drawPopular() {
    CopyOnWriteArrayList <FileNode> al=new CopyOnWriteArrayList<FileNode>();
    noStroke();
    textFont(font);
    textAlign(RIGHT, TOP);
    fill(255, 200);
    text("Popular Nodes (touches):", width-120, 0);
    for (FileNode fn : nodes.values()) {
      if (fn.qualifies()) {
        // Insertion Sort
        if (al.size() > 0) {
          int j = 0;
          for (; j < al.size(); j++) {
            if (fn.compareTo(al.get(j)) <= 0) {
              continue;
            } else {
              break;
            }
          }
          al.add(j,fn);
        } else {
          al.add(fn);
        }
      }
    }

    int i = 1;
    ListIterator<FileNode> it = al.listIterator();
    while (it.hasNext()) {
      FileNode n = it.next();
      // Limit to the top 10.
      if (i <= 10) {
        text(n.name + "  (" + n.touches + ")", width-100, 10 * i++);
      } else if (i > 10) {
        break;
      }
    }
  }

  /**
   * @param name
   * @return physics engine instance
   */
  @SuppressWarnings("unchecked")
  public PhysicsEngine getPhysicsEngine(String name) {
    PhysicsEngine pe = null;
    try {
      Class<PhysicsEngine> c = (Class<PhysicsEngine>)Class.forName(name);
      Constructor<PhysicsEngine> peConstructor = c.getConstructor();
      pe = peConstructor.newInstance();
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(1);
    }

    return pe;
  }

  /**
   *  Take screenshot
   */
  public void dumpFrame() {
    if (frameCount < maxFramesSaved) {
      final String outputFileName = insertFrame(SCREENSHOT_FILE);
      final PImage image = get();

      backgroundExecutor.execute(new Runnable() {
        public void run() {
          image.save(new File(outputFileName).getAbsolutePath());
        }
      });
    //  saveFrame(SCREENSHOT_FILE);
    }
  }

  /**
   *  Update the particle positions
   */
  public void update() {
    // Create a new histogram line
    ColorBins cb = new ColorBins();
    history.add(cb);

    // Create a new people histogram line
    peopleHistory.add(new Integer(livingPeople.size()));

    nextDate = new Date(prevDate.getTime() + UPDATE_DELTA);
    currentEvent = eventsQueue.peek();

    while (currentEvent != null && currentEvent.date.before(nextDate)) {
      if (finishedLoading) {
        currentEvent = eventsQueue.poll();
        if (currentEvent == null)
          return;
        }
        else {
          try {
            currentEvent = eventsQueue.take();
          } catch (InterruptedException e) {
          // TODO Auto-generated catch block
          System.out.println("Interrupted while fetching current event from eventsQueue");
          e.printStackTrace();
          continue;
        }
      }

      FileNode n = findNode(currentEvent.path + currentEvent.filename);
      if (n == null) {
        n = new FileNode(currentEvent);
        nodes.put(currentEvent.path + currentEvent.filename, n);
        livingNodes.addLast(n);
      } else {
        if (!n.isAlive())
            livingNodes.addLast(n);
        n.freshen();
      }

      // add to histogram
      cb.add(n.nodeHue);

      PersonNode p = findPerson(currentEvent.author);
      if (p == null) {
        p = new PersonNode(currentEvent.author);
        people.put(currentEvent.author, p);
        livingPeople.addLast(p);
      } else {
        if (!p.isAlive())
            livingPeople.addLast(p);
        p.freshen();
      }
      p.addColor(n.nodeHue);

      Edge ped = findEdge(n, p);
      if (ped == null) {
        ped = new Edge(n, p);
        edges.put(new Pair<FileNode,PersonNode>(n,p), ped);
        livingEdges.addLast(ped);
      } else {
        if (!ped.isAlive())
            livingEdges.addLast(ped);
        ped.freshen();
      }

      /*
       * if ( currentEvent.date.equals( prevDate ) ) { Edge e = findEdge( n, prevNode
       * ); if ( e == null ) { e = new Edge( n, prevNode ); edges.add( e ); } else {
       * e.freshen(); } }
       */

      // prevDate = currentEvent.date;
      prevNode = n;
      if (finishedLoading)
        currentEvent = eventsQueue.peek();
      else {
        while (eventsQueue.isEmpty());
        currentEvent = eventsQueue.peek();
      }
    }

    if ( !coolDown )
      prevDate = nextDate;

    // sort colorbins
    cb.sort();

    // restrict history to drawable area
    while (history.size() > 320)
      history.remove();

    // restrict people history to drawable area
    while (peopleHistory.size() > 200)
	peopleHistory.remove();

    // Do not allow toggle Physics Engine yet.
    safeToToggle = false;

    // Init frame:
    mPhysicsEngine.initializeFrame();

    // update velocity
    livingEdges = mPhysicsEngine.onRelaxEdges(livingEdges);

    // update velocity
    livingNodes = mPhysicsEngine.onRelaxNodes(livingNodes);

    // update velocity
    livingPeople = mPhysicsEngine.onRelaxPeople(livingPeople);

    // update position
    livingEdges = mPhysicsEngine.onUpdateEdges(livingEdges);

    // update position
    livingNodes = mPhysicsEngine.onUpdateNodes(livingNodes);

    // update position
    livingPeople = mPhysicsEngine.onUpdatePeople(livingPeople);

    // Finalize frame:
    mPhysicsEngine.finalizeFrame();

    safeToToggle = true;
    if (wantToToggle == true) {
      switchPhysicsEngine(toggleDirection);
    }
  }

  /**
   * Checks the node list for signs of life.
   * @return Does life exist?
   */
  public boolean isThereLife() {
      return !livingNodes.isEmpty();
  }

  /**
   * Searches the nodes array for a given name
   * @param name
   * @return FileNode with matching name or null if not found.
   */
  public FileNode findNode(String name) {
    return nodes.get(name);
  }

  /**
   * Searches the nodes array for a given name
   * @param n1 From
   * @param n2 To
   * @return Edge connecting n1 to n2 or null if not found
   */
  public Edge findEdge(FileNode n1, PersonNode n2) {
    return edges.get(new Pair<FileNode, PersonNode>(n1,n2));
  }

  /**
   * Searches the people array for a given name.
   * @param name
   * @return PersonNode for given name or null if not found.
   */
  public PersonNode findPerson(String name) {
    return people.get(name);
  }

  /**
   *  Load the standard event-formatted file.
   *  @param filename
   */
  public void loadRepEvents(String filename) {
    final String fullFilename = filename;

    Runnable eventLoader = new XMLQueueLoader(fullFilename, eventsQueue, isInputSorted);

    if (isInputSorted)
      backgroundExecutor.execute(eventLoader);
    else
      //we have to load all of the data before we can continue if it isn't sorted
      eventLoader.run();
  }

  /*
   * Output file events for debugging void printQueue() { while(
   * eventsQueue.size() > 0 ) { FileEvent fe = (FileEvent)eventsQueue.poll();
   * println( fe.date ); } }
   */

  /**
   * @note Keystroke callback function
   */
  public void keyPressed() {
    switch (key) {
      case ' ': {
        pauseButton();
        break;
      }
      case 'a': {
        drawNamesHalos = !drawNamesHalos;
        break;
      }
      case 'b': {
        showDebug = !showDebug;
        break;
      }
      case 'd': {
        showDate = !showDate;
        break;
      }
      case 'e' : {
        showEdges = !showEdges;
        break;
      }
      case 'E' : {
        showEngine = !showEngine;
        break;
      }
      case 'f' : {
        drawFilesFuzzy = !drawFilesFuzzy;
        break;
      }
      case 'h': {
        showHistogram = !showHistogram;
        break;
      }
      case 'j' : {
        drawFilesJelly = !drawFilesJelly;
        break;
      }
      case 'l': {
        showLegend = !showLegend;
        break;
      }
      case 'p': {
        showPopular = !showPopular;
        break;
      }
      case 'q': {
        exit();
        break;
      }
      case 's': {
        drawNamesSharp = !drawNamesSharp;
        break;
      }
      case 'S': {
        drawFilesSharp = !drawFilesSharp;
        break;
      }
      case '-': {
        wantToToggle = true;
        toggleDirection = false;
        break;
      }
      case '+': {
        wantToToggle = true;
        toggleDirection = true;
        break;
      }
      case '?': {
        showHelp = !showHelp;
        break;
      }
    }
  }

  /**
   * Method to switch between Physics Engines
   * @param direction Indicates whether or not to go left or right on the list
   */
  public void switchPhysicsEngine(boolean direction) {
    if (mPhysicsEngineChoices.size() > 1 && safeToToggle) {
      boolean found = false;
      for (int i = 0; i < mPhysicsEngineChoices.size() && !found; i++) {
        if (mPhysicsEngineChoices.get(i).pe == mPhysicsEngine) {
          found = true;
          wantToToggle = false;
          if (direction == true) {
            if ((i+1) < mPhysicsEngineChoices.size()) {
              mPhysicsEngine=mPhysicsEngineChoices.get(i+1).pe;
              physicsEngineSelection=mPhysicsEngineChoices.get(i+1).name;
            } else {
              mPhysicsEngine=mPhysicsEngineChoices.get(0).pe;
              physicsEngineSelection=mPhysicsEngineChoices.get(0).name;
            }
          } else {
            if ((i-1) >= 0) {
              mPhysicsEngine=mPhysicsEngineChoices.get(i-1).pe;
              physicsEngineSelection=mPhysicsEngineChoices.get(i-1).name;
            } else {
              mPhysicsEngine=mPhysicsEngineChoices.get(mPhysicsEngineChoices.size()-1).pe;
              physicsEngineSelection=mPhysicsEngineChoices.get(mPhysicsEngineChoices.size()-1).name;
            }
          }
        }
      }
    }
  }

  /**
   *  Toggle pause
   */
  public void pauseButton() {
    if (looping)
      noLoop();
    else
      loop();
    looping = !looping;
  }

  private class XMLQueueLoader implements Runnable {
    private final String fullFilename;
    private BlockingQueue<FileEvent> queue;
    boolean isXMLSorted;

    private XMLQueueLoader(String fullFilename, BlockingQueue<FileEvent> queue, boolean isXMLSorted) {
      this.fullFilename = fullFilename;
      this.queue = queue;
      this.isXMLSorted = isXMLSorted;
    }

    public void run(){
      XMLReader reader = null;
      try {
        reader = XMLReaderFactory.createXMLReader();
      } catch (SAXException e) {
        System.out.println("Couldn't find/create an XML SAX Reader");
        e.printStackTrace();
        System.exit(1);
      }
      reader.setContentHandler(new DefaultHandler(){
        public void startElement(String uri, String localName, String name,
            Attributes atts) throws SAXException {
          if (name.equals("event")){
            String eventFilename = atts.getValue("filename");
            String eventDatestr = atts.getValue("date");
            long eventDate = Long.parseLong(eventDatestr);
            String eventWeightStr = atts.getValue("weight");
            int eventWeight = 1;
            if (eventWeightStr != null) {
              eventWeight = Integer.parseInt(eventWeightStr);
            }

            //It's difficult for the user to tell that they're missing events,
            //so we should crash in this case
            if (isXMLSorted){
              if (eventDate < maximumDateSeenSoFar){
                System.out.println("Input not sorted, you must set IsInputSorted to false in your config file");
                System.exit(1);
              }
              else
                maximumDateSeenSoFar = eventDate;
            }

            String eventAuthor = atts.getValue("author");
            // int eventLinesAdded = atts.getValue( "linesadded" );
            // int eventLinesRemoved = atts.getValue( "linesremoved" );

            FileEvent evt = new FileEvent(eventDate, eventAuthor, "", eventFilename, eventWeight);
            try {
              queue.put(evt);
            } catch (InterruptedException e) {
              // TODO Auto-generated catch block
              System.out.println("Interrupted while trying to put into eventsQueue");
              e.printStackTrace();
              System.exit(1);
            }
          }
        }
        public void endDocument(){
          finishedLoading = true;
        }
      });
      try {
        reader.parse(fullFilename);
      } catch (Exception e) {
        // TODO Auto-generated catch block
        System.out.println("Error parsing xml:");
        e.printStackTrace();
        System.exit(1);
      }
    }
  }

  class Utils {
    Utils () {
    }
    /**
       * Draws a point.
       * @param x
       * @param y
       * @param red
       * @param green
       * @param blue
       */
    public void drawPoint (int x, int y, int red, int green, int blue) {
      noStroke();
      colorMode(RGB);
      stroke(red, green, blue);
      point(x, y);
    }

    /**
       * Draws a line.
       * @param fromX
       * @param fromY
       * @param toX
       * @param toY
       * @param red
       * @param green
       * @param blue
       */
    public void drawLine (int fromX, int fromY, int toX, int toY, int red, int green, int blue) {
      noStroke();
      colorMode(RGB);
      stroke(red, green, blue);
      strokeWeight(1.5f);
      line(fromX, fromY, toX, toY);
    }
  }

  /**
   * Class to associate the Physics Engine name to the
   * Physics Engine interface
   */
  class peConfig {
    protected String name;
    protected PhysicsEngine pe;

    peConfig(String n, PhysicsEngine p) {
      name = n;
      pe = p;
    }
  }


  /**
   * Describe an event on a file
   */
  class FileEvent implements Comparable<Object> {
    Date date;
    String author;
    String filename;
    String path;
    //int linesadded;
    //int linesremoved;
    int weight;

    /**
     * short constructor with base data
     */
    FileEvent(long datenum, String author, String path, String filename) {
      this(datenum, author, path, filename, 1);
    }

    /**
     * constructor with weight
     */
    FileEvent(long datenum, String author, String path, String filename, int weight) {
      this.date = new Date(datenum);
      this.author = author;
      this.path = path;
      this.filename = filename;
      this.weight = weight;
    }

    /**
     * Comparing two events by date (Not Used)
     * @param o
     * @return -1 if <, 0 if =, 1 if >
     */
    public int compareTo(Object o) {
      return date.compareTo(((FileEvent) o).date);
    }
  }

  /**
   * Base class for all drawable objects
   *
   *        Lists and implements features common to all drawable objects
   *        Edge and Node, FileNode and PersonNode
   */
  abstract class Drawable {
    public int life;

    final public int LIFE_INIT;
    final public int LIFE_DECREMENT;
    /**
     * 1) constructor(s)
     *
     * Init jobs common to all objects
     */
    Drawable(int lifeInit, int lifeDecrement) {
      // save config vars
      LIFE_INIT      = lifeInit;
      LIFE_DECREMENT = lifeDecrement;
      // init life relative vars
      life           = LIFE_INIT;
    }

    /**
     *  4) shortening life.
     */
    public boolean decay() {
      if (isAlive()) {
        life += LIFE_DECREMENT;
        if (life < 0) {
          life = 0;
        }
      }
      return life > 0;
    }

    /**
     * 5) drawing the new state => done in derived class.
     */
    public abstract void draw();

    /**
     * 6) reseting life as if new.
     */
    public abstract void freshen();

    /**
     * @return true if life > 0
     */
    public boolean isAlive() {
      return life > 0;
    }

  }

  /**
   * An Edge link two nodes together : a File to a Person.
   */
  class Edge extends Drawable {
    protected FileNode nodeFrom;
    protected PersonNode nodeTo;
    protected float len;

    /**
     * 1) constructor.
     * @param from FileNode
     * @param to PersonNode
     */
    Edge(FileNode from, PersonNode to) {
      super(EDGE_LIFE_INIT, EDGE_LIFE_DECREMENT);
      this.nodeFrom = from;
      this.nodeTo   = to;
      this.len      = EDGE_LEN;  // 25
    }

    /**
     * 5) drawing the new state.
     */
    public void draw() {
      if (life > 240) {
        stroke(255, life);
        strokeWeight(0.35f);
        line(nodeFrom.mPosition.x, nodeFrom.mPosition.y, nodeTo.mPosition.x, nodeTo.mPosition.y);
      }
    }

    public void freshen() {
      life = EDGE_LIFE_INIT;
    }
  }

  /**
   * A node is an abstraction for a File or a Person.
   */
  public abstract class Node extends Drawable {
    protected String name;
    protected Vector2f mPosition;
    protected Vector2f mSpeed;
    protected float maxSpeed = DEFAULT_NODE_SPEED;

    /**
     * mass of the node
     */
    protected float mass;

    /**
     * 1) constructor.
     */
    Node(int lifeInit, int lifeDecrement) {
      super(lifeInit, lifeDecrement);
      mPosition = new Vector2f();
      mSpeed = new Vector2f();
    }

  }

  /**
   * A node describing a file, which is repulsed by other files.
   */
  class FileNode extends Node implements Comparable<FileNode> {
    private int nodeHue;
    private int minBold;
    protected int touches;

    /**
     * @return file node as a string
     */
    public String toString() {
      return "FileNode{" + "name='" + name + '\'' + ", nodeHue=" + nodeHue + ", touches=" + touches + '}';
    }

    /**
     * 1) constructor.
     */
    FileNode(FileEvent fe) {
      super(FILE_LIFE_INIT, FILE_LIFE_DECREMENT); // 255, -2
      name = fe.path + fe.filename;
      touches = fe.weight;
      life = FILE_LIFE_INIT;
      colorMode(RGB);
      minBold = (int)(FILE_LIFE_INIT * ((100.0f - HIGHLIGHT_PCT)/100));
      nodeHue = colorAssigner.getColor(name);
      mass = FILE_MASS;
      maxSpeed = DEFAULT_FILE_SPEED;
      mPosition.set(mPhysicsEngine.fStartLocation());
      mSpeed.set(mPhysicsEngine.fStartVelocity(mass));
    }

    /**
     * 5) drawing the new state.
     */
    public void draw() {
      if (life > 0) {
        if (drawFilesSharp) {
          drawSharp();
        }
        if (drawFilesFuzzy) {
          drawFuzzy();
        }
        if (drawFilesJelly) {
          drawJelly();
        }

        /** TODO : this would become interesting on some special event, or for special materials
         * colorMode( RGB ); fill( 0, life ); textAlign( CENTER, CENTER ); text( name, x, y );
         * Example below:
         */
        if (showPopular) {
          textAlign( CENTER, CENTER );
          if (this.qualifies()) {
            text(touches, mPosition.x, mPosition.y - (8 + (int)Math.sqrt(touches)));
          }
        }
      }
    }

    /**
     * 6) reseting life as if new.
     */
    public void freshen() {
      life = FILE_LIFE_INIT;
      if (++touches > maxTouches) {
        maxTouches = touches;
      }
    }

    /**
     * reset life and add event weight to touches
     */
    public void freshen( FileEvent fe ) {
      life = FILE_LIFE_INIT;
      touches += fe.weight;

      // do not allow negative touches
      if ( touches < 0 )
        touches = 0;
      if ( touches > maxTouches )
        maxTouches = touches;
    }

    public boolean qualifies() {
      if (this.touches >= (maxTouches * 0.5f)) {
        return true;
      }
      return false;
    }

    public int compareTo(FileNode fn) {
      int retval = 0;
      if (this.touches < fn.touches) {
        retval = -1;
      } else if (this.touches > fn.touches) {
        retval = 1;
      }
      return retval;
    }

    public void drawSharp() {
      colorMode(RGB);
      fill(nodeHue, life);
      float w = 3;

      if (life >= minBold) {
        stroke(255, 128);
        w *= 2;
      } else {
        noStroke();
      }

      ellipseMode(CENTER);
      ellipse(mPosition.x, mPosition.y, w, w);
    }

    public void drawFuzzy() {
      tint(nodeHue, life);

      float w = 8 + (sqrt(touches) * 4);
      // not used float dubw = w * 2;
      float halfw = w / 2;
      if (life >= minBold) {
        colorMode(HSB);
        tint(hue(nodeHue), saturation(nodeHue) - 192, 255, life);
        // image( sprite, x - w, y - w, dubw, dubw );
      }
      // else
      image(sprite, mPosition.x - halfw, mPosition.y - halfw, w, w);
    }

    public void drawJelly() {
      noFill();
      if (life >= minBold)
        stroke(255);
      else
        stroke(nodeHue, life);
      float w = sqrt(touches);
      ellipseMode(CENTER);
      ellipse(mPosition.x, mPosition.y, w, w);
    }
  }

  /**
   * A node describing a person, which is repulsed by other persons.
   */
  class PersonNode extends Node {
    private int flavor = color(0);
    private int colorCount = 1;
    private int minBold;
    protected int touches;

    /**
     * 1) constructor.
     */
    PersonNode(String n) {
      super(PERSON_LIFE_INIT, PERSON_LIFE_DECREMENT); // -1
      maxSpeed = DEFAULT_PERSON_SPEED;
      name = n;
      minBold = (int)(PERSON_LIFE_INIT * (1 - ((float) HIGHLIGHT_PCT)/100));
      mass = PERSON_MASS; // bigger mass to person then to node, to stabilize them
      touches = 1;
      mPosition.set(mPhysicsEngine.pStartLocation());
      mSpeed.set(mPhysicsEngine.pStartVelocity(mass));
    }

    /**
     * 5) drawing the new state.
     */
    public void draw() {
      if (isAlive()) {
        textAlign(CENTER, CENTER);

        /** TODO: proportional font size, or light intensity,
                  or some sort of thing to disable the flashing */
        if (life >= minBold)
          textFont(boldFont);
        else
          textFont(font);

        text(name, mPosition.x, mPosition.y);
      }
    }

    public void freshen () {
      life = PERSON_LIFE_INIT;
      touches++;
    }

    public void addColor(int c) {
      colorMode(RGB);
      flavor = lerpColor(flavor, c, 1.0f / colorCount);
      colorCount++;
    }
  }

  /**
   * code_swarm Entry point.
   * @param args : should be the path to the config file
   */
  static public void main(String args[]) {
    try {
      if (args.length > 0) {
        System.out.println("code_swarm is free software: you can redistribute it and/or modify");
        System.out.println("it under the terms of the GNU General Public License as published by");
        System.out.println("the Free Software Foundation, either version 3 of the License, or");
        System.out.println("(at your option) any later version.");
        System.out.flush();
        cfg = new CodeSwarmConfig(args[0]);
        PApplet.main(new String[] { "code_swarm" });
      } else {
        System.err.println("Specify a config file.");
      }
    } catch (IOException e) {
      System.err.println("Failed due to exception: " + e.getMessage());
    }
  }
  /**
   * the alternative entry-point for code_swarm. It gets called from
   * {@link MainView} after fetching the repository log.
   * @param config the modified config
   *        (it's InputFile-property has been changed to reflect the
   *        fetched repository-log)
   */
  public static void start(CodeSwarmConfig config){
    cfg = config;
    PApplet.main(new String[]{"code_swarm"});
  }
}
