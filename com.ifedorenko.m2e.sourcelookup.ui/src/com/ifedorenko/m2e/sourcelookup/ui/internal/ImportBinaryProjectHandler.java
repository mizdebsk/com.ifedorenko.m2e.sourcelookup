package com.ifedorenko.m2e.sourcelookup.ui.internal;

import java.io.File;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.DebugException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.ui.handlers.HandlerUtil;

import com.ifedorenko.m2e.binaryproject.AbstractBinaryProjectsImportJob;
import com.ifedorenko.m2e.sourcelookup.internal.JDIHelpers;
import com.ifedorenko.m2e.sourcelookup.internal.PomPropertiesScanner;
import com.ifedorenko.m2e.sourcelookup.internal.SourceLookupParticipant;

public class ImportBinaryProjectHandler
    extends AbstractHandler
{
    @Override
    public Object execute( ExecutionEvent event )
        throws ExecutionException
    {
        ISelection selection = HandlerUtil.getCurrentSelectionChecked( event );

        if ( selection instanceof IStructuredSelection && !selection.isEmpty() )
        {
            try
            {
                importBinaryProjects( ( (IStructuredSelection) selection ).getFirstElement() );
            }
            catch ( DebugException e )
            {
                throw new ExecutionException( "Could not import binary project", e );
            }
        }

        return null;
    }

    public static void importBinaryProjects( final Object debugElement )
        throws DebugException
    {

        final File location = JDIHelpers.getLocation( debugElement );

        if ( location == null )
        {
            return;
        }

        Job job = new AbstractBinaryProjectsImportJob()
        {
            @Override
            protected IStatus run( IProgressMonitor monitor )
            {
                IStatus status = super.run( monitor );

                if ( status.isOK() )
                {
                    SourceLookupParticipant sourceLookup = SourceLookupParticipant.getSourceLookup( debugElement );

                    try
                    {
                        sourceLookup.getSourceContainer( debugElement, true, monitor );
                    }
                    catch ( CoreException e )
                    {
                        status = e.getStatus();
                    }
                }

                return status;
            }

            @Override
            protected List<ArtifactKey> getArtifactKeys()
                throws CoreException
            {
                return new PomPropertiesScanner<ArtifactKey>()
                {
                    @Override
                    protected ArtifactKey visitProject( IProject project )
                    {
                        return null;
                    }

                    @Override
                    protected ArtifactKey visitMavenProject( IMavenProjectFacade mavenProject )
                    {
                        return null;
                    }

                    @Override
                    protected ArtifactKey visitArtifact( ArtifactKey artifact )
                        throws CoreException
                    {
                        return artifact;
                    }
                }.scan( location );
            }
        };
        job.setUser( true );
        job.schedule();
    }
}
