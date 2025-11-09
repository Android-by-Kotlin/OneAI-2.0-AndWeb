import axios from 'axios';

// ImgBB API key - Free tier allows 5000 uploads per month
const IMGBB_API_KEY = '8a493c2d8f3b7f4c0e1e5a9b6c7d8e9f'; // You can get your own key at https://api.imgbb.com/

interface ImgBBResponse {
  data: {
    url: string;
    display_url: string;
    delete_url: string;
  };
  success: boolean;
  status: number;
}

/**
 * Upload image file to ImgBB and return public URL
 */
export async function uploadImageToImgBB(file: File): Promise<string> {
  try {
    console.log('Uploading image to ImgBB...');
    
    // Convert file to base64
    const base64String = await fileToBase64(file);
    
    // Remove data:image prefix if present
    const base64Data = base64String.includes('base64,') 
      ? base64String.split('base64,')[1] 
      : base64String;
    
    // Create form data
    const formData = new FormData();
    formData.append('image', base64Data);
    formData.append('key', IMGBB_API_KEY);
    
    // Upload to ImgBB
    const response = await axios.post<ImgBBResponse>(
      'https://api.imgbb.com/1/upload',
      formData,
      {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
        timeout: 30000, // 30 seconds
      }
    );
    
    if (response.data.success && response.data.data.url) {
      console.log('Image uploaded successfully:', response.data.data.url);
      return response.data.data.url;
    }
    
    throw new Error('Failed to upload image to ImgBB');
  } catch (error: any) {
    console.error('Image upload error:', error);
    
    if (error.response?.status === 400) {
      throw new Error('Invalid image file. Please try a different image.');
    }
    if (error.response?.status === 403) {
      throw new Error('Image upload service limit reached. Please try again later.');
    }
    if (error.code === 'ECONNABORTED') {
      throw new Error('Upload timeout. Please try a smaller image.');
    }
    
    throw new Error('Failed to upload image. Please try again.');
  }
}

/**
 * Convert File to base64 string
 */
function fileToBase64(file: File): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onloadend = () => {
      if (reader.result) {
        resolve(reader.result as string);
      } else {
        reject(new Error('Failed to read file'));
      }
    };
    reader.onerror = () => reject(new Error('Failed to read file'));
    reader.readAsDataURL(file);
  });
}

/**
 * Alternative: Upload to ModelsLab directly (if they support it)
 * This is a placeholder - ModelsLab doesn't have a direct upload endpoint
 */
export async function uploadImageToModelsLab(file: File): Promise<string> {
  // ModelsLab doesn't have a direct upload API
  // We need to use ImgBB or another service
  return uploadImageToImgBB(file);
}
