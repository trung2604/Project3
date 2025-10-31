import axios from 'axios';
import { API_ENDPOINTS } from '../constants.js';

const CLOUDINARY_URL = 'https://api.cloudinary.com/v1_1/dswb2h4ny/image/upload';

class CloudinaryService {
    /**
     * Get signature from backend
     */
    async getSignature() {
        try {
            const url = `http://localhost:8080${API_ENDPOINTS.CLOUDINARY.SIGNATURE}`;
            const response = await axios.get(url);
            return response.data;
        } catch (error) {
            console.error('Error getting signature:', error);
            console.error('Error response:', error.response?.data);
            throw error;
        }
    }

    /**
     * Upload image to Cloudinary
     */
    async uploadImage(file) {
        try {
            // Get signature from backend
            const signature = await this.getSignature();

            // Validate signature data
            if (!signature.apiKey || !signature.timestamp || !signature.signature) {
                throw new Error('Invalid signature data from backend');
            }

            // Create form data with signature
            const formData = new FormData();
            formData.append('file', file);
            formData.append('api_key', signature.apiKey);
            formData.append('timestamp', signature.timestamp);
            formData.append('signature', signature.signature);
            formData.append('folder', 'restaurant-menu');

            // Upload to Cloudinary
            const response = await axios.post(CLOUDINARY_URL, formData, {
                headers: {
                    'Content-Type': 'multipart/form-data',
                },
            });

            return {
                url: response.data.secure_url,
                publicId: response.data.public_id,
            };
        } catch (error) {
            console.error('Error uploading image:', error);
            console.error('Error response:', error.response?.data);
            console.error('Error details:', JSON.stringify(error.response?.data, null, 2));
            throw error;
        }
    }
}

export const cloudinaryService = new CloudinaryService();
