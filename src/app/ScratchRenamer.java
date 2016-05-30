package app;


import gui.entry.CheckEntry;
import gui.entry.DirectoryEntry;
import gui.entry.Entry;
import gui.props.UIEntryProps;
import gui.props.variable.BooleanVariable;
import gui.props.variable.IntVariable;
import gui.props.variable.StringVariable;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import process.NonStandardProcess;
import process.ProcessManager;
import process.io.ProcessStreamSiphon;
import statics.FileUtils;
import statics.GUIUtils;
import statics.StringUtils;
import ui.log.LogDialog;
import ui.log.LogFileSiphon;

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
		this.setSize( new Dimension( 420, 270 ) );
		this.setDefaultCloseOperation( EXIT_ON_CLOSE );
		this.setLayout( new BorderLayout() );
		
		props.addVariable( "sourceDir", new StringVariable( "D:/" ) );
		props.addVariable( "logName", new StringVariable( "scratchRename" ) );
		props.addVariable( "series", new StringVariable() );
		props.addVariable( "season", new IntVariable( 1 ) );
		props.addVariable( "starting", new IntVariable( 1 ) );
		props.addVariable( "process", new StringVariable( "scratchRename" ) );
		props.addVariable( "titleOnly", new BooleanVariable( false ) );
		
		rename = new ScratchRename( props.getString( "process" ) );
		this.add( dirPanel(), BorderLayout.CENTER );
		JButton b = new JButton( "Run Rename" );
		b.addActionListener( e -> rename.execute() );
		this.add( b, BorderLayout.SOUTH );
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
		p.add( new Entry( "Log Name:", props.getVariable( "logName" ), new Dimension( GUIUtils.SHORT ) ) );
		GUIUtils.spacer( p );
		p.add( new CheckEntry( "Title Only", props.getVariable( "titleOnly" ), new Dimension( GUIUtils.SHORT ) ) );
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
				rename( f );
				sendMessage( "Completed Renaming files in: " + f.getAbsolutePath() );
				log.notifyProcessEnded( name );
				ProcessManager.getInstance().removeAll( name );
			} catch ( Exception e ) {
				e.printStackTrace();
			}
			for ( ProcessStreamSiphon siphon : ProcessManager.getInstance().getSiphons( name ) ) {
				siphon.notifyProcessEnded( name );
			}
		}
		
		private void rename( File f ) {
			int i = (Integer)props.getVariable( "starting" ).getValue();
			Map<String, File> map = new HashMap<>();
			for ( File d : f.listFiles() ) {
				if ( d.isFile() && !d.getName().endsWith( ".log" ) && !d.getName().equals( "Thumbs.db" ) ) {
					map.put( d.getName(), d );
				}
			}
			List<String> keys = new ArrayList<>( map.keySet() );
			Collections.sort( keys, new Comparator<String>() {
			    public int compare(String o1, String o2) {
			        return extractInt(o1) - extractInt(o2);
			    }

			    int extractInt(String s) {
			        String num = s.replaceAll("\\D", "");
			        // return 0 if no digits found
			        return num.isEmpty() ? 0 : Integer.parseInt(num);
			    }
			} );
			for ( String s : keys ) {
				File d = map.get( s );
				File n = new File( f.getAbsolutePath() + "/" + props.getString( "series" ) + 
						( ( (Boolean)props.getVariable( "titleOnly" ).getValue() ) ?
						d.getName().substring( d.getName().lastIndexOf( "_s" ) )
						: "_s" + StringUtils.addZeroes( Integer.parseInt( props.getString( "season" ) ) ) + "e" + StringUtils.addZeroes( i ) + "." + FileUtils.getSuffix( d ) ) );
				sendMessage( "Renaming: " + d.getAbsolutePath() + " ---> " + n.getAbsolutePath() );
				d.renameTo( n );
				i++;
			}
		}
	}
}