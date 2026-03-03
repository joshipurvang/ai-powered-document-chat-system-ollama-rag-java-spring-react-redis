import React, { useState, useRef } from 'react';
import { Form, Button, Alert, ListGroup, ProgressBar, Badge } from 'react-bootstrap';
import { FaUpload, FaFile, FaTimes, FaCheckCircle, FaExclamationTriangle } from 'react-icons/fa';
import api from '../services/api';

/**
 * Document upload component
 * @param {Object} props - Component props
 * @param {string} props.sessionId - Current session ID
 * @param {Function} props.onUploadComplete - Callback when upload completes
 */
const DocumentUpload = ({ sessionId, onUploadComplete }) => {
  const [selectedFiles, setSelectedFiles] = useState([]);
  const [uploading, setUploading] = useState(false);
  const [uploadProgress, setUploadProgress] = useState(0);
  const [uploadResults, setUploadResults] = useState([]);
  const [error, setError] = useState('');
  
  const fileInputRef = useRef(null);

  const MAX_FILES = 3;
  const MAX_SIZE_MB = 5;
  const ALLOWED_TYPES = [
    'application/pdf',
    'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
    'application/msword',
    'application/vnd.openxmlformats-officedocument.presentationml.presentation',
    'application/vnd.ms-powerpoint',
    'text/plain'
  ];

  const getFileIcon = (type) => {
    if (type.includes('pdf')) return '📄';
    if (type.includes('word')) return '📝';
    if (type.includes('presentation')) return '📊';
    if (type.includes('text')) return '📃';
    return '📎';
  };

  const formatFileSize = (bytes) => {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i];
  };

  const validateFile = (file) => {
    if (!ALLOWED_TYPES.includes(file.type)) {
      return `${file.name}: Invalid file type. Only PDF, DOCX, PPTX, and TXT files are allowed.`;
    }
    
    if (file.size > MAX_SIZE_MB * 1024 * 1024) {
      return `${file.name}: File size exceeds ${MAX_SIZE_MB}MB limit.`;
    }
    
    return null;
  };

  const handleFileSelect = (e) => {
    const files = Array.from(e.target.files);
    setError('');
    setUploadResults([]);
    
    if (selectedFiles.length + files.length > MAX_FILES) {
      setError(`You can only upload up to ${MAX_FILES} files at a time.`);
      return;
    }
    
    const validationErrors = [];
    const validFiles = [];
    
    files.forEach(file => {
      const error = validateFile(file);
      if (error) {
        validationErrors.push(error);
      } else {
        validFiles.push(file);
      }
    });
    
    if (validationErrors.length > 0) {
      setError(validationErrors.join('\n'));
    }
    
    if (validFiles.length > 0) {
      setSelectedFiles(prev => [...prev, ...validFiles]);
    }
    
    // Reset file input
    if (fileInputRef.current) {
      fileInputRef.current.value = '';
    }
  };

  const handleRemoveFile = (index) => {
    setSelectedFiles(prev => prev.filter((_, i) => i !== index));
  };

  const handleUpload = async () => {
    if (selectedFiles.length === 0) {
      setError('Please select at least one file to upload.');
      return;
    }

    setUploading(true);
    setUploadProgress(0);
    setError('');

    try {
      const formData = new FormData();
      selectedFiles.forEach(file => {
        formData.append('files', file);
      });

      const response = await api.uploadDocuments(sessionId, formData, (progress) => {
        setUploadProgress(progress);
      });

      if (response.data.success) {
        const results = response.data.data.documents;
        setUploadResults(results);
        
        const successfulUploads = results.filter(r => r.processed);
        if (successfulUploads.length > 0) {
          setTimeout(() => {
            onUploadComplete(successfulUploads);
          }, 1500);
        }
      } else {
        setError(response.data.error || 'Upload failed');
      }
    } catch (err) {
      console.error('Upload error:', err);
      setError('Failed to upload documents. Please try again.');
    } finally {
      setUploading(false);
    }
  };

  return (
    <div className="document-upload">
      {error && (
        <Alert variant="danger" className="mb-3">
          <FaExclamationTriangle className="me-2" />
          {error}
        </Alert>
      )}

      {uploadResults.length > 0 && (
        <Alert variant="info" className="mb-3">
          <h6>Upload Results:</h6>
          {uploadResults.map((result, index) => (
            <div key={index} className="mb-1">
              {result.processed ? (
                <span className="text-success">
                  <FaCheckCircle className="me-2" />
                  {result.fileName} - Success
                </span>
              ) : (
                <span className="text-danger">
                  <FaExclamationTriangle className="me-2" />
                  {result.fileName} - {result.error}
                </span>
              )}
            </div>
          ))}
        </Alert>
      )}

      <div className="upload-area mb-3">
        <Form.Group>
          <Form.Label className="d-flex align-items-center mb-3">
            <FaFile className="me-2" />
            <strong>Select Documents (Max {MAX_FILES} files, {MAX_SIZE_MB}MB each)</strong>
          </Form.Label>
          
          <div className="file-input-wrapper">
            <input
              ref={fileInputRef}
              type="file"
              multiple
              accept=".pdf,.doc,.docx,.ppt,.pptx,.txt"
              onChange={handleFileSelect}
              disabled={uploading || selectedFiles.length >= MAX_FILES}
              className="file-input"
              id="fileInput"
            />
            <label htmlFor="fileInput" className="file-input-label">
              <FaUpload className="me-2" />
              Choose Files
            </label>
          </div>
          
          <Form.Text className="text-muted d-block mt-2">
            Supported formats: PDF, DOCX, PPTX, TXT
          </Form.Text>
        </Form.Group>
      </div>

      {selectedFiles.length > 0 && (
        <div className="mb-3">
          <h6 className="mb-2">Selected Files ({selectedFiles.length}/{MAX_FILES})</h6>
          <ListGroup>
            {selectedFiles.map((file, index) => (
              <ListGroup.Item 
                key={index}
                className="d-flex justify-content-between align-items-center"
              >
                <div className="d-flex align-items-center flex-grow-1">
                  <span className="file-icon me-2">{getFileIcon(file.type)}</span>
                  <div>
                    <div className="file-name">{file.name}</div>
                    <small className="text-muted">{formatFileSize(file.size)}</small>
                  </div>
                </div>
                <Button
                  variant="outline-danger"
                  size="sm"
                  onClick={() => handleRemoveFile(index)}
                  disabled={uploading}
                >
                  <FaTimes />
                </Button>
              </ListGroup.Item>
            ))}
          </ListGroup>
        </div>
      )}

      {uploading && (
        <div className="mb-3">
          <ProgressBar 
            now={uploadProgress} 
            label={`${uploadProgress}%`}
            animated 
            striped
          />
        </div>
      )}

      <div className="d-grid">
        <Button
          variant="primary"
          size="lg"
          onClick={handleUpload}
          disabled={uploading || selectedFiles.length === 0}
          className="upload-btn"
        >
          {uploading ? (
            <>
              <span className="spinner-border spinner-border-sm me-2" />
              Uploading & Processing...
            </>
          ) : (
            <>
              <FaUpload className="me-2" />
              Upload & Process Documents
            </>
          )}
        </Button>
      </div>
    </div>
  );
};

export default DocumentUpload;