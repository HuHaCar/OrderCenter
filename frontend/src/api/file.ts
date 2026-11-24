import http, { unwrap } from './http'
import type { FileAsset } from '@/types/order'

export function uploadFile(
  file: File,
  onProgress?: (percent: number) => void,
): Promise<FileAsset> {
  const formData = new FormData()
  formData.append('file', file)

  return unwrap<FileAsset>(
    http.post('/files/upload', formData, {
      timeout: 0,
      onUploadProgress: (event) => {
        if (event.total) {
          onProgress?.(Math.round((event.loaded / event.total) * 100))
        }
      },
    }),
  )
}

export async function downloadFile(fileId: number): Promise<Blob> {
  const response = await http.get(`/files/${fileId}/download`, {
    responseType: 'blob',
    timeout: 0,
  })
  return response.data as Blob
}

export async function previewFile(fileId: number): Promise<Blob> {
  const response = await http.get(`/files/${fileId}/preview`, {
    responseType: 'blob',
    timeout: 0,
  })
  return response.data as Blob
}
