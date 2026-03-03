import React, { useState, useEffect, useRef } from 'react';
import { Container, Row, Col, Card } from 'react-bootstrap';
import { ToastContainer, toast } from 'react-toastify';
//import SockJS from 'sockjs-client';
//import { Stomp } from 'stompjs';
import { Stomp } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import 'bootstrap/dist/css/bootstrap.min.css';
import 'react-toastify/dist/ReactToastify.css';
import './App.css';

import UserInfoForm from './components/UserInfoForm';
import DocumentUpload from './components/DocumentUpload';
import ChatInterface from './components/ChatInterface';
import SessionInfo from './components/SessionInfo';
import api from './services/api';

/**
 * Main application component for Document Chat AI.
 * Manages session state, WebSocket connections, and component orchestration.
 * 
 * @component
 * @returns {JSX.Element} The main application UI
 */
function App() {
  const [session, setSession] = useState(null);
  const [documents, setDocuments] = useState([]);
  const [messages, setMessages] = useState([]);
  const [connected, setConnected] = useState(false);
  const [currentStep, setCurrentStep] = useState('userInfo'); // userInfo, upload, chat
  
  const stompClient = useRef(null);

  /**
   * Initializes WebSocket connection when session is created
   */
  useEffect(() => {
    if (session && !connected) {
      connectWebSocket();
    }

    return () => {
      disconnectWebSocket();
    };
  }, [session]);

  /**
   * Establishes WebSocket connection for real-time chat updates
   */
  const connectWebSocket = () => {
    const socket = new SockJS('http://localhost:8080/api/ws');
    stompClient.current = Stomp.over(socket);
    
    stompClient.current.connect({}, (frame) => {
      console.log('Connected to WebSocket:', frame);
      setConnected(true);
      
      stompClient.current.subscribe(`/topic/chat/${session.sessionId}`, (message) => {
        const chatMessage = JSON.parse(message.body);
        setMessages(prev => [...prev, chatMessage]);
      });
      
      toast.success('Connected to chat server');
    }, (error) => {
      console.error('WebSocket connection error:', error);
      toast.error('Failed to connect to chat server');
      setConnected(false);
    });
  };

  /**
   * Disconnects WebSocket connection
   */
  const disconnectWebSocket = () => {
    if (stompClient.current && connected) {
      stompClient.current.disconnect();
      setConnected(false);
    }
  };

  /**
   * Handles user information submission and session creation
   * @param {Object} userInfo - User information (name, phone, deviceInfo)
   */
  const handleUserInfoSubmit = async (userInfo) => {
    try {
      const response = await api.createSession(userInfo);
      
      if (response.data.success) {
        setSession(response.data.data);
        setCurrentStep('upload');
        toast.success('Session created successfully!');
      } else {
        toast.error(response.data.error || 'Failed to create session');
      }
    } catch (error) {
      console.error('Error creating session:', error);
      toast.error('Failed to create session. Please try again.');
    }
  };

  /**
   * Handles document upload completion
   * @param {Array} uploadedDocs - Array of uploaded document metadata
   */
  const handleDocumentsUploaded = (uploadedDocs) => {
    setDocuments(uploadedDocs);
    setCurrentStep('chat');
    toast.success('Documents uploaded successfully! You can now start chatting.');
  };

  /**
   * Handles sending chat messages
   * @param {string} question - User's question
   */
  const handleSendMessage = async (question) => {
    if (!session) {
      toast.error('No active session');
      return;
    }

    try {
      const response = await api.sendQuery(session.sessionId, question);
      
      if (!response.data.success) {
        toast.error(response.data.error || 'Failed to get response');
      }
    } catch (error) {
      console.error('Error sending message:', error);
      toast.error('Failed to send message. Please try again.');
    }
  };

  /**
   * Handles ending the chat session
   * @param {boolean} shouldContinue - Whether user wants to continue chatting
   */
  const handleEndSession = async (shouldContinue) => {
    if (!shouldContinue) {
      try {
        await api.clearDocuments(session.sessionId);
        await api.endSession(session.sessionId);
        
        disconnectWebSocket();
        setSession(null);
        setDocuments([]);
        setMessages([]);
        setCurrentStep('userInfo');
        
        toast.info('Session ended. Thank you for using Document Chat AI!');
      } catch (error) {
        console.error('Error ending session:', error);
        toast.error('Failed to end session properly');
      }
    }
  };

  /**
   * Handles exporting chat history
   */
  const handleExportHistory = async () => {
    try {
      const response = await api.exportChatHistory(session.sessionId);
      
      const blob = new Blob([response.data], { type: 'text/plain' });
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = `chat-history-${session.sessionId}.txt`;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);
      
      toast.success('Chat history exported successfully!');
    } catch (error) {
      console.error('Error exporting history:', error);
      toast.error('Failed to export chat history');
    }
  };

  return (
    <div className="App">
      <ToastContainer 
        position="top-right"
        autoClose={3000}
        hideProgressBar={false}
        newestOnTop
        closeOnClick
        pauseOnHover
      />
      
      <div className="app-header">
        <Container>
          <h1 className="text-center py-4">
            <span className="logo-icon">📚</span> Document Chat AI
          </h1>
          <p className="text-center text-muted">
            Upload your documents and chat with AI to get instant answers
          </p>
        </Container>
      </div>

      <Container className="mt-4 mb-5">
        {session && (
          <Row className="mb-4">
            <Col>
              <SessionInfo 
                session={session} 
                documentsCount={documents.length}
                onExportHistory={handleExportHistory}
              />
            </Col>
          </Row>
        )}

        <Row className="justify-content-center">
          <Col lg={10} xl={8}>
            {currentStep === 'userInfo' && (
              <Card className="shadow-sm animate-fade-in">
                <Card.Body className="p-4">
                  <h3 className="mb-4 text-center">Welcome! Let's Get Started</h3>
                  <UserInfoForm onSubmit={handleUserInfoSubmit} />
                </Card.Body>
              </Card>
            )}

            {currentStep === 'upload' && session && (
              <Card className="shadow-sm animate-fade-in">
                <Card.Body className="p-4">
                  <h3 className="mb-4 text-center">Upload Your Documents</h3>
                  <DocumentUpload 
                    sessionId={session.sessionId}
                    onUploadComplete={handleDocumentsUploaded}
                  />
                </Card.Body>
              </Card>
            )}

            {currentStep === 'chat' && session && (
              <Card className="shadow-sm animate-fade-in">
                <Card.Body className="p-0">
                  <ChatInterface
                    sessionId={session.sessionId}
                    messages={messages}
                    onSendMessage={handleSendMessage}
                    onEndSession={handleEndSession}
                    documents={documents}
                  />
                </Card.Body>
              </Card>
            )}
          </Col>
        </Row>
      </Container>

      <footer className="app-footer text-center py-3 mt-5">
        <Container>
          <small className="text-muted">
            Document Chat AI v1.0 | Powered by Ollama & Spring AI
          </small>
        </Container>
      </footer>
    </div>
  );
}

export default App;