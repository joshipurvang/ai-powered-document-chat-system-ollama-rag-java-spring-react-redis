import React, { useState } from 'react';
import { Form, Button, Row, Col } from 'react-bootstrap';
import { FaUser, FaPhone, FaMobileAlt } from 'react-icons/fa';

/**
 * User information form component
 * @param {Object} props - Component props
 * @param {Function} props.onSubmit - Callback when form is submitted
 */
const UserInfoForm = ({ onSubmit }) => {
  const [formData, setFormData] = useState({
    name: '',
    phoneNumber: '',
    deviceInfo: ''
  });

  const [errors, setErrors] = useState({});

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: value
    }));
    
    // Clear error when user types
    if (errors[name]) {
      setErrors(prev => ({
        ...prev,
        [name]: ''
      }));
    }
  };

  const validate = () => {
    const newErrors = {};
    
    if (!formData.name.trim()) {
      newErrors.name = 'Name is required';
    } else if (formData.name.length < 2) {
      newErrors.name = 'Name must be at least 2 characters';
    }
    
    if (!formData.phoneNumber.trim()) {
      newErrors.phoneNumber = 'Phone number is required';
    } else if (!/^[0-9]{10,15}$/.test(formData.phoneNumber)) {
      newErrors.phoneNumber = 'Invalid phone number (10-15 digits)';
    }
    
    return newErrors;
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    
    const validationErrors = validate();
    if (Object.keys(validationErrors).length > 0) {
      setErrors(validationErrors);
      return;
    }

    // Get device info
    const deviceInfo = formData.deviceInfo || navigator.userAgent;
    
    onSubmit({
      ...formData,
      deviceInfo
    });
  };

  return (
    <Form onSubmit={handleSubmit}>
      <Form.Group className="mb-3">
        <Form.Label>
          <FaUser className="me-2" />
          Full Name *
        </Form.Label>
        <Form.Control
          type="text"
          name="name"
          value={formData.name}
          onChange={handleChange}
          placeholder="Enter your name"
          isInvalid={!!errors.name}
          className="form-control-lg"
        />
        <Form.Control.Feedback type="invalid">
          {errors.name}
        </Form.Control.Feedback>
      </Form.Group>

      <Form.Group className="mb-3">
        <Form.Label>
          <FaPhone className="me-2" />
          Phone Number *
        </Form.Label>
        <Form.Control
          type="tel"
          name="phoneNumber"
          value={formData.phoneNumber}
          onChange={handleChange}
          placeholder="Enter 10-15 digit phone number"
          isInvalid={!!errors.phoneNumber}
          className="form-control-lg"
        />
        <Form.Control.Feedback type="invalid">
          {errors.phoneNumber}
        </Form.Control.Feedback>
        <Form.Text className="text-muted">
          Please enter numbers only (10-15 digits)
        </Form.Text>
      </Form.Group>

      <Form.Group className="mb-4">
        <Form.Label>
          <FaMobileAlt className="me-2" />
          Device Info (Optional)
        </Form.Label>
        <Form.Control
          type="text"
          name="deviceInfo"
          value={formData.deviceInfo}
          onChange={handleChange}
          placeholder="e.g., iPhone 12, Windows PC"
          className="form-control-lg"
        />
      </Form.Group>

      <div className="d-grid">
        <Button 
          variant="primary" 
          size="lg" 
          type="submit"
          className="submit-btn"
        >
          Start Session
        </Button>
      </div>
    </Form>
  );
};

export default UserInfoForm;