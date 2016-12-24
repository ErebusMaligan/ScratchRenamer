package app;


import java.awt.BorderLayout;
import java.awt.Dimension;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import comparator.WindowsExplorerFileComparator;
import gui.entry.CheckEntry;
import gui.entry.DirectoryEntry;
import gui.entry.Entry;
import gui.props.UIEntryProps;
import gui.props.variable.BooleanVariable;
import gui.props.variable.IntVariable;
import gui.props.variable.StringVariable;
import process.NonStandardProcess;
import process.ProcessManager;
import process.io.ProcessStreamSiphon;
import statics.FileUtils;
import statics.GUIUtils;
import statics.StringUtils;
import ui.log.LogDialog;
import ui.log.LogFileSiphon;
import xml.XMLUtils;

/**
 * @author Daniel J. Rivers
 *         2013
 *
 * Created: Aug 24, 2013, 6:14:01 PM
 */
public class ScratchRenamer extends JFrame {

	private static final long serialVersionUID = 1L;

	private UIEntryProps props = new UIEntryProps();
	
	private ScratchRename rename;

	public ScratchRenamer() {
		ImageIcon icon = new ImageIcon( getClass().getResource( "rename.png" ) );
		this.setTitle( "Scratch Renamer" );
		this.setIconImage( icon.getImage() );
		this.setDefaultCloseOperation( EXIT_ON_CLOSE );
		this.setLayout( new BorderLayout() );
		
		props.addVariable( "sourceDir", new StringVariable( "D:/" ) );
		props.addVariable( "logName", new StringVariable( "scratchRename" ) );
		props.addVariable( "series", new StringVariable() );
		props.addVariable( "season", new IntVariable( 1 ) );
		props.addVariable( "starting", new IntVariable( 1 ) );
		props.addVariable( "episode", new StringVariable( "" ) );
		props.addVariable( "partNum", new IntVariable( 1 ) );
		props.addVariable( "process", new StringVariable( "scratchRename" ) );
		props.addVariable( "rename", new BooleanVariable( true ) );
		props.addVariable( "nfo", new BooleanVariable( false ) );
		props.addVariable( "overwrite", new BooleanVariable( false ) );
		
		rename = new ScratchRename( props.getString( "process" ) );
		this.add( dirPanel(), BorderLayout.CENTER );
		JButton b = new JButton( "Run Rename" );
		b.addActionListener( e -> rename.execute() );
		this.add( b, BorderLayout.SOUTH );
		this.pack();
		this.setVisible( true );
	}

	private JPanel dirPanel() {
		JPanel p = new JPanel();
		p.setLayout( new BoxLayout( p, BoxLayout.Y_AXIS ) );
		p.add( new DirectoryEntry( "Source Dir:", props.getVariable( "sourceDir" ) ) );
		GUIUtils.spacer( p );
		p.add( new Entry( "Series:", props.getVariable( "series" ), new Dimension( GUIUtils.SHORT ) ) );
		GUIUtils.spacer( p );
		p.add( new Entry( "Season:", props.getVariable( "season" ), new Dimension( GUIUtils.SHORT ) ) );
		GUIUtils.spacer( p );
		p.add( new Entry( "Start #:", props.getVariable( "starting" ), new Dimension( GUIUtils.SHORT ) ) );
		GUIUtils.spacer( p );
		p.add( new Entry( "Episode Name:", props.getVariable( "episode" ), new Dimension( GUIUtils.SHORT ) ) );
		GUIUtils.spacer( p );
		p.add( new Entry( "Log Name:", props.getVariable( "logName" ), new Dimension( GUIUtils.SHORT ) ) );
		GUIUtils.spacer( p );
		p.add( new CheckEntry( "Rename Files", props.getVariable( "rename" ), new Dimension( GUIUtils.LONG ) ) );
		GUIUtils.spacer( p );
		p.add( new CheckEntry( "Generate NFO", props.getVariable( "nfo" ), new Dimension( GUIUtils.LONG ) ) );
		GUIUtils.spacer( p );
		p.add( new CheckEntry( "Overwrite Name", props.getVariable( "overwrite" ), new Dimension( GUIUtils.LONG ) ) );
		GUIUtils.spacer( p );
		p.add( new Entry( "Series Part #", props.getVariable( "partNum" ), new Dimension( GUIUtils.SHORT) ) );
		return p;
	}

	public static void main( String args[] ) {
		try {
			UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );
		} catch ( ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e ) {
			System.err.println( "Critical JVM Failure!" );
			e.printStackTrace();
		}
		new ScratchRenamer();
	}
	
	private class ScratchRename extends NonStandardProcess {

		public ScratchRename( String name ) {
			super( name );
		}
		
		public void execute() {
			try {
				String runName = props.getString( "process" );
				LogFileSiphon log = new LogFileSiphon( runName, props.getString( "sourceDir" ) + "/" + props.getString( "series" ) + "_" + props.getString( "season" ) + "_" + props.getString( "logName" ) + ".log" ) {
					public void skimMessage( String name, String s ) {
						try {
							fstream.write( "[" + sdf.format( new Date( System.currentTimeMillis() ) ) + "]:  " + s );
							fstream.newLine();
							fstream.flush();
						} catch ( IOException e ) {
							e.printStackTrace();
						}
					}
				};
				new LogDialog( ScratchRenamer.this, runName, false );
				File f = new File( props.getString( "sourceDir" ) );
				proc( f );
				
				log.notifyProcessEnded( name );
				ProcessManager.getInstance().removeAll( name );
			} catch ( Exception e ) {
				e.printStackTrace();
			}
			for ( ProcessStreamSiphon siphon : ProcessManager.getInstance().getSiphons( name ) ) {
				siphon.notifyProcessEnded( name );
			}
		}
		
		private void proc( File f ) {
			int i = (Integer)props.getVariable( "starting" ).getValue();
			List<File> files = Arrays.asList( f.listFiles() );
			files.sort( new WindowsExplorerFileComparator() );
			for ( File d : files ) {
				if ( d.isFile() && !d.getName().endsWith( ".log" ) && !d.getName().equals( "Thumbs.db" ) && !d.getName().endsWith( ".nfo" ) ) {

					if ( (Boolean)props.getVariable( "rename" ).getValue() ) {
						d = rename( f, d, i );
						i++;
					}

					if ( (Boolean)props.getVariable( "nfo" ).getValue() ) {
						rename.sendMessage( "Generating NFO File" );
						File out = new File( d.getAbsolutePath().substring( 0, d.getAbsolutePath().lastIndexOf( "." ) ) + ".nfo" );
						if ( !out.exists() ) {
							writeNFOFile( d, out );
						} else {
							rename.sendMessage( "nfo file exists for:  " + out.getName() + " skipping to next" );
						}
					}

				}
			}
			rename.sendMessage( "Completed processing files in: " + f.getAbsolutePath() );
		}
		
		private File rename( File f, File d, int i ) {
			String series = props.getString( "series" );
			String season = "S" + StringUtils.addZeroes( Integer.parseInt( props.getString( "season" ) ) );
			String episode = "E" + StringUtils.addZeroes( i );
			String suffix = FileUtils.getSuffix( d );
			String part = String.valueOf( i - (Integer)props.getVariable( "starting" ).getValue() + (Integer)props.getVariable( "partNum" ).getValue() );
			String episodeName = ( (Boolean)props.getVariable( "overwrite" ).getValue() ) ? props.getString( "episode" ) + " P" + part : getExistingEpisodeName( d );
			File n = new File( f.getAbsolutePath() + "/" + series + " - " + season + episode + " - " + episodeName + "." + suffix );
			sendMessage( "Renaming: " + d.getAbsolutePath() + " ---> " + n.getAbsolutePath() );
			d.renameTo( n );
			return n;
		}
		
		private String getExistingEpisodeName( File d ) {
			String ret = "";
			int s = d.getName().lastIndexOf( " - " );
			if ( s != -1 ) {
				s = s + 3;
				int e = d.getName().lastIndexOf( "." );
				if ( e != -1 ) {
					ret = d.getName().substring( s, e );
				}
			}
			return ret;
		}
		
		private void writeNFOFile( File in, File out ) {
			String[] full = in.getName().substring( 0, in.getName().lastIndexOf( "." ) ).split( "-" );
			String episode = "";
			String season = "";
			String title = full[ full.length - 1 ].trim();
			for ( String s : full ) {
				String t = s.trim();
				if ( t.matches( "[sS](\\d{2,})[eE](\\d{2,})" ) ) {
					int i = t.indexOf( "e" );
					if ( i == -1 ) {
						i = t.indexOf( "E" );
					}
					season = String.valueOf( Integer.parseInt( t.substring( 1, i ) ) );
					episode = String.valueOf( Integer.parseInt( t.substring( i + 1 ) ) );
				}
			}
			try ( PrintWriter write = new PrintWriter( out ) ) {
				write.print( XMLUtils.wrapInTag( "\n\t" + XMLUtils.wrapInTag( title, "title" ) + "\n\t" + XMLUtils.wrapInTag( season, "season" ) + "\n\t" + XMLUtils.wrapInTag( episode, "episode" ) + "\n", "episodedetails" ) );
				rename.sendMessage( "wrote nfo to:  " + out.getName() );
			} catch ( FileNotFoundException e ) {
				StringWriter sw = new StringWriter();
				e.printStackTrace( new PrintWriter( sw ) );
				rename.sendMessage( sw.toString() );
				e.printStackTrace();
			}
		}
	}
}