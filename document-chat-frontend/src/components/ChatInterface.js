// src/components/ChatInterface.js
import React, { useState, useRef, useEffect } from 'react';
import { Form, Button, Alert, Badge, Modal } from 'react-bootstrap';
import { FaPaperPlane, FaRobot, FaUser, FaBook, FaSignOutAlt } from 'react-icons/fa';

/**
 * Chat interface component for interacting with uploaded documents
 * @param {Object} props - Component props
 * @param {string} props.sessionId - Current session ID
 * @param {Array} props.messages - Chat messages
 * @param {Function} props.onSendMessage - Callback to send message
 * @param {Function} props.onEndSession - Callback to end session
 * @param {Array} props.documents - Uploaded documents
 */
const ChatInterface = ({ 
  sessionId, 
  messages, 
  onSendMessage, 
  onEndSession,
  documents 
}) => {
  const [inputMessage, setInputMessage] = useState('');
  const [showEndModal, setShowEndModal] = useState(false);
  const [isWaitingResponse, setIsWaitingResponse] = useState(false);
  const messagesEndRef = useRef(null);
  const chatContainerRef = useRef(null);

  /**
   * Auto-scroll to bottom when new messages arrive
   */
  useEffect(() => {
    scrollToBottom();
  }, [messages]);

  /**
   * Check if last message is from user (waiting for AI response)
   */
  useEffect(() => {
    if (messages.length > 0) {
      const lastMessage = messages[messages.length - 1];
      setIsWaitingResponse(lastMessage.fromUser);
    }
  }, [messages]);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  const handleSend = (e) => {
    e.preventDefault();
    
    if (!inputMessage.trim() || isWaitingResponse) {
      return;
    }

    onSendMessage(inputMessage.trim());
    setInputMessage('');
  };

  const handleAskMoreQuestions = () => {
    // Just a visual prompt, user can continue chatting
    setShowEndModal(false);
  };

  const handleEndChat = () => {
    setShowEndModal(true);
  };

  const confirmEndSession = (shouldContinue) => {
    setShowEndModal(false);
    onEndSession(shouldContinue);
  };

  const formatTimestamp = (timestamp) => {
    const date = new Date(timestamp);
    return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
  };

  return (
    <div className="chat-interface">
      {/* Chat Header */}
      <div className="chat-header p-3 border-bottom">
        <div className="d-flex justify-content-between align-items-center">
          <div>
            <h5 className="mb-1">AI Document Assistant</h5>
            <small className="text-muted">
              <FaBook className="me-1" />
              {documents.length} document(s) loaded
            </small>
          </div>
          <Button 
            variant="outline-danger" 
            size="sm"
            onClick={handleEndChat}
          >
            <FaSignOutAlt className="me-2" />
            End Chat
          </Button>
        </div>
      </div>

      {/* Messages Container */}
      <div 
        ref={chatContainerRef}
        className="chat-messages p-3"
        style={{ 
          height: '500px', 
          overflowY: 'auto',
          backgroundColor: '#f8f9fa'
        }}
      >
        {messages.length === 0 && (
          <Alert variant="info" className="text-center">
            <FaRobot size={40} className="mb-2" />
            <p className="mb-0">
              Welcome! I'm here to help you with your documents. 
              Ask me anything about the content you've uploaded!
            </p>
          </Alert>
        )}

        {messages.map((message, index) => (
          <div
            key={index}
            className={`message ${message.fromUser ? 'user-message' : 'ai-message'} mb-3`}
          >
            <div className="message-header mb-2">
              <div className="d-flex align-items-center">
                {message.fromUser ? (
                  <>
                    <FaUser className="me-2 text-primary" />
                    <strong>You</strong>
                  </>
                ) : (
                  <>
                    <FaRobot className="me-2 text-success" />
                    <strong>AI Assistant</strong>
                  </>
                )}
                <small className="text-muted ms-2">
                  {formatTimestamp(message.timestamp)}
                </small>
              </div>
            </div>
            
            <div className={`message-bubble ${message.fromUser ? 'user' : 'ai'}`}>
              <p className="mb-0">{message.content}</p>
              
              {!message.fromUser && message.sources && (
                <div className="mt-2 pt-2 border-top">
                  <small className="text-muted">
                    <strong>Sources:</strong> {message.sources}
                  </small>
                </div>
              )}
            </div>
          </div>
        ))}

        {isWaitingResponse && (
          <div className="message ai-message mb-3">
            <div className="message-header mb-2">
              <div className="d-flex align-items-center">
                <FaRobot className="me-2 text-success" />
                <strong>AI Assistant</strong>
              </div>
            </div>
            <div className="message-bubble ai">
              <div className="typing-indicator">
                <span></span>
                <span></span>
                <span></span>
              </div>
            </div>
          </div>
        )}

        <div ref={messagesEndRef} />
      </div>

      {/* Input Area */}
      <div className="chat-input p-3 border-top bg-white">
        <Form onSubmit={handleSend}>
          <div className="d-flex gap-2">
            <Form.Control
              type="text"
              placeholder="Ask a question about your documents..."
              value={inputMessage}
              onChange={(e) => setInputMessage(e.target.value)}
              disabled={isWaitingResponse}
              className="chat-input-field"
            />
            <Button
              type="submit"
              variant="primary"
              disabled={!inputMessage.trim() || isWaitingResponse}
              className="send-btn"
            >
              <FaPaperPlane />
            </Button>
          </div>
        </Form>
        
        <div className="mt-2">
          <small className="text-muted">
            💡 Tip: Ask specific questions about your documents for better answers
          </small>
        </div>
      </div>

      {/* End Session Modal */}
      <Modal 
        show={showEndModal} 
        onHide={() => setShowEndModal(false)}
        centered
      >
        <Modal.Header closeButton>
          <Modal.Title>Continue Chatting?</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <p>Do you want to ask more questions about your documents?</p>
          <Alert variant="warning" className="mb-0">
            <small>
              If you choose "No", the session will end and all uploaded documents 
              will be removed from the system.
            </small>
          </Alert>
        </Modal.Body>
        <Modal.Footer>
          <Button 
            variant="success" 
            onClick={handleAskMoreQuestions}
            size="lg"
          >
            Yes, Continue Chatting
          </Button>
          <Button 
            variant="danger" 
            onClick={() => confirmEndSession(false)}
          >
            No, End Session
          </Button>
        </Modal.Footer>
      </Modal>
    </div>
  );
};

export default ChatInterface;