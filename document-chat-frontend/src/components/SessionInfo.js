import React from 'react';
import { Card, Badge, Button } from 'react-bootstrap';
import { FaUser, FaClock, FaFileAlt, FaDownload } from 'react-icons/fa';

/**
 * Session information display component
 * @param {Object} props - Component props
 * @param {Object} props.session - Session data
 * @param {number} props.documentsCount - Number of uploaded documents
 * @param {Function} props.onExportHistory - Callback to export chat history
 */
const SessionInfo = ({ session, documentsCount, onExportHistory }) => {
  const formatDate = (dateString) => {
    const date = new Date(dateString);
    return date.toLocaleString();
  };

  return (
    <Card className="session-info-card">
      <Card.Body className="d-flex justify-content-between align-items-center">
        <div className="d-flex flex-wrap gap-3">
          <div className="d-flex align-items-center">
            <FaUser className="text-primary me-2" />
            <span><strong>{session.userName}</strong></span>
          </div>
          
          <div className="d-flex align-items-center">
            <FaClock className="text-muted me-2" />
            <small className="text-muted">
              Started: {formatDate(session.createdAt)}
            </small>
          </div>
          
          <div className="d-flex align-items-center">
            <FaFileAlt className="text-success me-2" />
            <Badge bg="success">{documentsCount} Document(s)</Badge>
          </div>
        </div>
        
        <Button 
          variant="outline-primary" 
          size="sm"
          onClick={onExportHistory}
          className="d-flex align-items-center"
        >
          <FaDownload className="me-2" />
          Export History
        </Button>
      </Card.Body>
    </Card>
  );
};

export default SessionInfo;