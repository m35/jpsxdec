
package jpsxdec.cdreaders.iso9660;

import java.io.File;

public class ISOFile extends File {

    private final long m_iStartSector;
    private final long m_iSize;
    
    public ISOFile(File oDir, String sFileName, long iStartSector, long iSize) {
        super(oDir, sFileName);
        m_iStartSector = iStartSector;
        m_iSize = iSize;
    }

    public long getLength() {
        return (m_iSize+2047) / 2048;
    }

    public long getEndSector() {
        return m_iStartSector + getLength() - 1;
    }
    
    public long getStartSector() {
        return m_iStartSector;
    }
    
}
